package io.bluetape4k.batch.benchmark.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.batch.benchmark.support.BenchmarkDatabase
import io.bluetape4k.batch.benchmark.support.BenchmarkSourceTable
import io.bluetape4k.batch.benchmark.support.BenchmarkTargetTable
import io.bluetape4k.batch.benchmark.support.BenchmarkSourceRecord
import io.bluetape4k.batch.benchmark.support.BenchmarkTargetRecord
import io.bluetape4k.batch.benchmark.support.KeyRange
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchJobRepository
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchReader
import io.bluetape4k.batch.jdbc.ExposedJdbcBatchWriter
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * JDBC 기반 배치 벤치마크 공용 인프라.
 *
 * ## JMH 라이프사이클 설계 원칙
 *
 * JMH는 동일 `@State` 객체에 정의된 모든 `@Setup`/`@TearDown` 메서드를 같은 Level에서
 * 모두 호출합니다. 따라서 **시드 시나리오**와 **엔드 투 엔드 시나리오**를 하나의 상태 객체에
 * 묶으면 DataSource 이중 초기화와 스키마 조작 순서 충돌이 발생합니다.
 *
 * 이를 방지하기 위해 두 시나리오의 라이프사이클을 별도의 `@State` 클래스로 분리합니다.
 * - [AbstractJdbcSeedState]: 시드(INSERT) 시나리오 전용 상태 — 자체 Trial/Iteration 라이프사이클 포함
 * - [AbstractJdbcJobState]: 엔드 투 엔드 시나리오 전용 상태 — 자체 Trial/Iteration 라이프사이클 포함
 *
 * 구체 벤치마크 클래스([H2JdbcBatchBenchmark] 등)는 각 State의 구체 서브클래스를 정의하고,
 * `@Benchmark` 메서드의 파라미터로 해당 구체 State 타입을 직접 선언합니다.
 * JMH는 파라미터 타입의 구체 클래스를 기준으로 State를 해석하므로, 추상 타입이 아닌
 * 구체 타입을 파라미터로 사용해야 합니다.
 *
 * ## 측정 경계
 * - **측정 외부(TearDown/Setup)**: Testcontainers 기동, DataSource/커넥션 풀 생성, 스키마 생성
 * - **시드 벤치마크 본문**: 소스 행 INSERT만 측정
 * - **엔드 투 엔드 벤치마크 본문**: 소스 데이터 적재 완료 후 배치 잡 1회 실행만 측정
 *
 * ## 리포지터리 전략
 * 시퀀셜(`parallelism=1`)·병렬(`parallelism>1`) 경로 모두 [ExposedJdbcBatchJobRepository]를
 * 사용합니다. 동일한 조건에서 `parallelism` 파라미터의 효과만을 측정할 수 있습니다.
 *
 * ## 구체 클래스
 * [H2JdbcBatchBenchmark], [PostgreSqlJdbcBatchBenchmark], [MySqlJdbcBatchBenchmark]
 */
object JdbcBenchmarkInfra {
    internal val ALL_TABLES: Array<Table> = arrayOf(
        BatchJobExecutionTable, BatchStepExecutionTable,
        BenchmarkSourceTable, BenchmarkTargetTable,
    )

    internal const val CHUNK_SIZE = 500
    internal const val INSERT_CHUNK_SIZE = 1_000
}

// ── 공유 JDBC 인프라 헬퍼 함수 ────────────────────────────────────────────

/**
 * HikariCP DataSource와 Exposed Database를 생성합니다.
 */
internal fun buildDataSourceAndDatabase(
    jdbcUrl: String,
    driverClassName: String,
    username: String,
    password: String,
    poolSize: Int,
    poolName: String,
    initSql: String = "",
): Pair<HikariDataSource, Database> {
    val config = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        this.driverClassName = driverClassName
        this.username = username
        this.password = password
        maximumPoolSize = poolSize
        minimumIdle = minOf(2, poolSize)
        connectionTimeout = 30_000
        initializationFailTimeout = 60_000
        this.poolName = poolName
        if (initSql.isNotBlank()) connectionInitSql = initSql
    }
    val ds = HikariDataSource(config)
    val db = Database.connect(ds)
    return ds to db
}

/**
 * 모든 테이블을 삭제하고 재생성합니다.
 */
internal fun resetSchema(db: Database) {
    transaction(db) {
        SchemaUtils.drop(*JdbcBenchmarkInfra.ALL_TABLES)
        SchemaUtils.create(*JdbcBenchmarkInfra.ALL_TABLES)
    }
}

/**
 * 작업 테이블(대상 + 잡 실행 테이블)을 비웁니다.
 */
internal fun truncateWorkingTables(db: Database) {
    transaction(db) {
        SchemaUtils.drop(BatchJobExecutionTable, BatchStepExecutionTable, BenchmarkTargetTable)
        SchemaUtils.create(BatchJobExecutionTable, BatchStepExecutionTable, BenchmarkTargetTable)
    }
}

/**
 * 소스 테이블에 [dataSize]건의 레코드를 삽입합니다.
 */
internal fun seedSourceRows(db: Database, dataSize: Int) {
    (1..dataSize).chunked(JdbcBenchmarkInfra.INSERT_CHUNK_SIZE).forEach { chunk ->
        transaction(db) {
            BenchmarkSourceTable.batchInsert(chunk) { i ->
                this[BenchmarkSourceTable.name] = "item-$i"
                this[BenchmarkSourceTable.value] = i
            }
        }
    }
}

/**
 * 소스 테이블의 최소/최대 키를 집계 함수(MIN, MAX) 단일 쿼리로 조회합니다.
 *
 * 두 번의 정렬 스캔 대신 단일 집계 쿼리를 사용하여 측정 외부 비용을 최소화합니다.
 * 집계 표현식은 반드시 변수에 저장한 뒤 `select`와 `row[]` 모두에 동일 인스턴스를
 * 전달해야 합니다 (Exposed의 Expression 동등성 기준).
 */
internal fun minMaxKey(db: Database): Pair<Long, Long> {
    return transaction(db) {
        val minExpr = BenchmarkSourceTable.id.min()
        val maxExpr = BenchmarkSourceTable.id.max()
        val row = BenchmarkSourceTable.select(minExpr, maxExpr).single()
        val min = row[minExpr] ?: error("소스 테이블이 비어 있습니다")
        val max = row[maxExpr] ?: error("소스 테이블이 비어 있습니다")
        min to max
    }
}

/**
 * 키 범위를 [parallelism] 개 파티션으로 분할합니다.
 */
internal fun partitionRanges(db: Database, parallelism: Int): List<KeyRange> {
    parallelism.requirePositiveNumber("parallelism")
    val (minKey, maxKey) = minMaxKey(db)
    check(maxKey >= minKey) {
        "유효하지 않은 키 범위: minKey=$minKey > maxKey=$maxKey"
    }
    val totalKeys = maxKey - minKey + 1
    val partitionCount = minOf(parallelism.toLong(), totalKeys).toInt()
    val baseSize = totalKeys / partitionCount
    val remainder = totalKeys % partitionCount

    var previousMax: Long? = null
    var nextMin = minKey
    return MutableList(partitionCount) { index ->
        val currentSize = baseSize + if (index.toLong() < remainder) 1 else 0
        val currentMax = nextMin + currentSize - 1
        KeyRange(previousMax, currentMax).also {
            previousMax = currentMax
            nextMin = currentMax + 1
        }
    }
}

/**
 * 단일 파티션 배치 잡을 실행하고 쓰기 건수를 반환합니다.
 *
 * 시퀀셜·병렬 양쪽 경로 모두 [ExposedJdbcBatchJobRepository]를 사용하여
 * 동일한 조건에서 `parallelism` 효과만을 측정합니다.
 */
internal fun runJobPartition(
    db: Database,
    jobName: String,
    minKey: Long? = null,
    maxKey: Long? = null,
): Int {
    val job = batchJob(jobName) {
        repository(ExposedJdbcBatchJobRepository(db, CheckpointJson.jackson3()))
        step<BenchmarkSourceRecord, BenchmarkTargetRecord>("readAndWrite") {
            reader(buildReader(db, minKey, maxKey))
            processor { src -> BenchmarkTargetRecord(src.name.uppercase(), src.value * 2) }
            writer(buildWriter(db))
            chunkSize(JdbcBenchmarkInfra.CHUNK_SIZE)
        }
    }
    var writeCount = 0
    runSuspendIO {
        val report = job.run()
        writeCount = report.stepReports.sumOf { it.writeCount.toInt() }
    }
    return writeCount
}

/**
 * 배치 잡을 병렬 실행하고 총 쓰기 건수를 반환합니다.
 *
 * 시퀀셜 경로와 동일하게 [ExposedJdbcBatchJobRepository]를 사용합니다.
 */
internal fun runParallelJobs(db: Database, parallelism: Int): Int {
    val ranges = partitionRanges(db, parallelism)
    var totalWriteCount = 0
    runSuspendIO {
        coroutineScope {
            ranges.mapIndexed { index, range ->
                async {
                    runJobPartition(
                        db,
                        "jdbcBenchmarkJob-partition-$index",
                        range.minKeyExclusive,
                        range.maxKeyInclusive,
                    )
                }
            }.awaitAll().also { counts ->
                totalWriteCount = counts.sum()
            }
        }
    }
    return totalWriteCount
}

private fun buildReader(
    db: Database,
    minKey: Long? = null,
    maxKey: Long? = null,
): ExposedJdbcBatchReader<Long, BenchmarkSourceRecord> =
    ExposedJdbcBatchReader(
        database = db,
        table = BenchmarkSourceTable,
        keyColumn = BenchmarkSourceTable.id,
        pageSize = JdbcBenchmarkInfra.CHUNK_SIZE,
        rowMapper = { row ->
            BenchmarkSourceRecord(
                id = row[BenchmarkSourceTable.id],
                name = row[BenchmarkSourceTable.name],
                value = row[BenchmarkSourceTable.value],
            )
        },
        keyExtractor = { it.id },
        minKey = minKey,
        maxKey = maxKey,
    )

private fun buildWriter(db: Database): ExposedJdbcBatchWriter<BenchmarkTargetRecord> =
    ExposedJdbcBatchWriter(db, BenchmarkTargetTable, ignore = true) { record ->
        this[BenchmarkTargetTable.sourceName] = record.sourceName
        this[BenchmarkTargetTable.transformedValue] = record.transformedValue
    }

// ── 추상 State 베이스 클래스 ──────────────────────────────────────────────

/**
 * 시드(INSERT) 시나리오 전용 JMH State 추상 클래스.
 *
 * ## 라이프사이클
 * - **Trial Setup** ([trialSetup]): DataSource를 생성하고 스키마를 초기화합니다.
 * - **Iteration Setup** ([iterationSetup]): 각 반복 전에 스키마를 재초기화합니다.
 *   INSERT 없이 비어 있는 테이블에서만 측정하기 위함입니다.
 * - **Trial TearDown** ([trialTearDown]): DataSource를 닫습니다.
 *
 * 구체 클래스는 DB별 연결 정보와 `@Param` 필드를 제공하고, JMH `@Setup`/`@TearDown` 어노테이션이
 * 붙은 위임 메서드를 노출합니다.
 */
abstract class AbstractJdbcSeedState {

    companion object : KLogging()

    /** 대상 데이터베이스 식별자. */
    abstract val database: BenchmarkDatabase

    /** JDBC URL. Testcontainers 사용 시 컨테이너 기동 포함. */
    protected abstract fun jdbcUrl(): String

    /** JDBC 드라이버 클래스 이름. */
    protected abstract fun driverClassName(): String

    /** DB 접속 사용자 이름. */
    protected abstract fun dbUser(): String

    /** DB 접속 비밀번호. */
    protected abstract fun dbPassword(): String

    /** 연결 후 실행할 초기화 SQL. 없으면 빈 문자열. */
    protected open fun initSql(): String = ""

    private var dataSource: HikariDataSource? = null
    internal var exposedDatabase: Database? = null

    /** 적재할 소스 레코드 건수. 구체 클래스의 `@Param` 필드로 주입됩니다. */
    abstract val dataSize: Int

    /** HikariCP 최대 커넥션 수. 구체 클래스의 `@Param` 필드로 주입됩니다. */
    abstract val poolSize: Int

    /**
     * Trial 시작 시 DataSource를 생성하고 스키마를 초기화합니다.
     *
     * 구체 클래스의 `@Setup(Level.Trial)` 메서드에서 호출합니다.
     * JMH 측정에 포함되지 않습니다.
     */
    protected fun trialSetup() {
        log.info { "[${database.displayName}] Seed Trial setup 시작: poolSize=$poolSize" }
        val (ds, db) = buildDataSourceAndDatabase(
            jdbcUrl = jdbcUrl(),
            driverClassName = driverClassName(),
            username = dbUser(),
            password = dbPassword(),
            poolSize = poolSize,
            poolName = "seed-jdbc-${database.slug}",
            initSql = initSql(),
        )
        dataSource = ds
        exposedDatabase = db
        resetSchema(db)
        log.info { "[${database.displayName}] Seed Trial setup 완료" }
    }

    /**
     * 각 반복 전에 스키마를 재초기화합니다.
     *
     * 구체 클래스의 `@Setup(Level.Iteration)` 메서드에서 호출합니다.
     * JMH 측정에 포함되지 않습니다.
     */
    protected fun iterationSetup() {
        resetSchema(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" })
    }

    /**
     * Trial 종료 시 DataSource를 닫습니다.
     *
     * 구체 클래스의 `@TearDown(Level.Trial)` 메서드에서 호출합니다.
     * JMH 측정에 포함되지 않습니다.
     */
    protected fun trialTearDown() {
        runCatching {
            transaction(exposedDatabase!!) { SchemaUtils.drop(*JdbcBenchmarkInfra.ALL_TABLES) }
        }
        runCatching { dataSource?.close() }
        log.info { "[${database.displayName}] Seed Trial teardown 완료" }
    }

    /**
     * 소스 테이블에 [dataSize]건의 레코드를 삽입하고 삽입 건수를 반환합니다.
     *
     * `@Benchmark` 메서드의 본문에서 호출됩니다 (측정 대상).
     */
    fun insertSourceRows(): Int {
        seedSourceRows(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" }, dataSize)
        return dataSize
    }
}

/**
 * 엔드 투 엔드 시나리오 전용 JMH State 추상 클래스.
 *
 * ## 라이프사이클
 * - **Trial Setup** ([trialSetup]): DataSource를 생성하고 스키마를 초기화한 뒤 소스 데이터를 적재합니다.
 * - **Iteration Setup** ([iterationSetup]): 각 반복 전에 작업 테이블(대상 + 잡 실행)을 비웁니다.
 *   소스 테이블은 그대로 유지하여 재적재 비용이 측정에 포함되지 않습니다.
 * - **Trial TearDown** ([trialTearDown]): DataSource를 닫습니다.
 *
 * ## 리포지터리 전략
 * 시퀀셜(`parallelism=1`)·병렬(`parallelism>1`) 양쪽 경로 모두 [ExposedJdbcBatchJobRepository]를
 * 사용합니다. 동일한 조건에서 `parallelism` 파라미터의 효과만을 독립적으로 측정할 수 있습니다.
 *
 * 구체 클래스는 DB별 연결 정보와 `@Param` 필드를 제공하고, JMH `@Setup`/`@TearDown` 어노테이션이
 * 붙은 위임 메서드를 노출합니다.
 */
abstract class AbstractJdbcJobState {

    companion object : KLogging()

    /** 대상 데이터베이스 식별자. */
    abstract val database: BenchmarkDatabase

    /** JDBC URL. Testcontainers 사용 시 컨테이너 기동 포함. */
    protected abstract fun jdbcUrl(): String

    /** JDBC 드라이버 클래스 이름. */
    protected abstract fun driverClassName(): String

    /** DB 접속 사용자 이름. */
    protected abstract fun dbUser(): String

    /** DB 접속 비밀번호. */
    protected abstract fun dbPassword(): String

    /** 연결 후 실행할 초기화 SQL. 없으면 빈 문자열. */
    protected open fun initSql(): String = ""

    private var dataSource: HikariDataSource? = null
    internal var exposedDatabase: Database? = null

    /** 처리할 소스 레코드 건수. 구체 클래스의 `@Param` 필드로 주입됩니다. */
    abstract val dataSize: Int

    /** HikariCP 최대 커넥션 수. 구체 클래스의 `@Param` 필드로 주입됩니다. */
    abstract val poolSize: Int

    /** 병렬 파티션 수. 구체 클래스의 `@Param` 필드로 주입됩니다. */
    abstract val parallelism: Int

    /**
     * Trial 시작 시 DataSource를 생성하고 스키마를 초기화한 뒤 소스 데이터를 적재합니다.
     *
     * 구체 클래스의 `@Setup(Level.Trial)` 메서드에서 호출합니다.
     * JMH 측정에 포함되지 않습니다.
     */
    protected fun trialSetup() {
        log.info { "[${database.displayName}] Job Trial setup 시작: poolSize=$poolSize, dataSize=$dataSize" }
        val (ds, db) = buildDataSourceAndDatabase(
            jdbcUrl = jdbcUrl(),
            driverClassName = driverClassName(),
            username = dbUser(),
            password = dbPassword(),
            poolSize = poolSize,
            poolName = "job-jdbc-${database.slug}",
            initSql = initSql(),
        )
        dataSource = ds
        exposedDatabase = db
        resetSchema(db)
        seedSourceRows(db, dataSize)
        log.info { "[${database.displayName}] Job Trial setup 완료: ${dataSize}건 적재 완료" }
    }

    /**
     * 각 반복 전에 작업 테이블(대상 + 잡 실행 테이블)을 비웁니다.
     *
     * 소스 테이블은 유지하여 재적재 비용이 측정에 포함되지 않습니다.
     * 구체 클래스의 `@Setup(Level.Iteration)` 메서드에서 호출합니다.
     */
    protected fun iterationSetup() {
        truncateWorkingTables(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" })
    }

    /**
     * Trial 종료 시 DataSource를 닫습니다.
     *
     * 구체 클래스의 `@TearDown(Level.Trial)` 메서드에서 호출합니다.
     * JMH 측정에 포함되지 않습니다.
     */
    protected fun trialTearDown() {
        runCatching {
            transaction(exposedDatabase!!) { SchemaUtils.drop(*JdbcBenchmarkInfra.ALL_TABLES) }
        }
        runCatching { dataSource?.close() }
        log.info { "[${database.displayName}] Job Trial teardown 완료" }
    }

    /**
     * 배치 잡을 실행하고 기록된 총 쓰기 건수를 반환합니다.
     *
     * `@Benchmark` 메서드의 본문에서 호출됩니다 (측정 대상).
     * `parallelism=1`이면 순차 실행, `parallelism>1`이면 키 범위 파티셔닝 기반 병렬 실행입니다.
     * 두 경로 모두 [ExposedJdbcBatchJobRepository]를 사용하므로 공정한 비교가 가능합니다.
     */
    fun runJob(): Int {
        val db = requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" }
        return if (parallelism <= 1) {
            runJobPartition(db, "jdbcBenchmarkJob")
        } else {
            runParallelJobs(db, parallelism)
        }
    }
}
