package io.bluetape4k.batch.benchmark.r2dbc

import io.bluetape4k.batch.benchmark.support.BenchmarkDatabase
import io.bluetape4k.batch.benchmark.support.BenchmarkSourceRecord
import io.bluetape4k.batch.benchmark.support.BenchmarkSourceTable
import io.bluetape4k.batch.benchmark.support.BenchmarkTargetRecord
import io.bluetape4k.batch.benchmark.support.BenchmarkTargetTable
import io.bluetape4k.batch.benchmark.support.KeyRange
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.batch.r2dbc.ExposedR2dbcBatchJobRepository
import io.bluetape4k.batch.r2dbc.ExposedR2dbcBatchReader
import io.bluetape4k.batch.r2dbc.ExposedR2dbcBatchWriter
import io.bluetape4k.exposed.tests.Containers
import io.bluetape4k.exposed.tests.TestDBConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.support.requirePositiveNumber
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * R2DBC 기반 배치 벤치마크 공용 인프라입니다.
 *
 * JDBC benchmark와 동일하게 시드/엔드투엔드 시나리오를 별도 `@State` 클래스로 분리합니다.
 * R2DBC 환경에서는 `ConnectionPool`과 `R2dbcDatabase`를 Trial lifecycle 동안 유지하고,
 * 측정 대상에서는 실제 INSERT 또는 배치 잡 실행만 수행합니다.
 */
object R2dbcBenchmarkInfra {
    internal val ALL_TABLES: Array<Table> = arrayOf(
        BatchJobExecutionTable,
        BatchStepExecutionTable,
        BenchmarkSourceTable,
        BenchmarkTargetTable,
    )

    internal const val CHUNK_SIZE = 500
    internal const val INSERT_CHUNK_SIZE = 1_000
}

internal fun buildPoolAndDatabase(
    url: String,
    poolSize: Int,
    initialSize: Int,
    configure: R2dbcDatabaseConfig.Builder.() -> Unit = {},
): Pair<ConnectionPool, R2dbcDatabase> {
    poolSize.requirePositiveNumber("poolSize")
    initialSize.requirePositiveNumber("initialSize")

    val options = ConnectionFactoryOptions.parse(url)
    val connectionFactory = ConnectionFactories.get(options)
    val pool = ConnectionPool(
        ConnectionPoolConfiguration.builder(connectionFactory)
            .maxSize(poolSize)
            .initialSize(initialSize)
            .build(),
    )
    val database = R2dbcDatabase.connect(
        pool,
        databaseConfig = R2dbcDatabaseConfig {
            connectionFactoryOptions = options
            configure()
        },
    )
    return pool to database
}

internal suspend fun resetSchema(database: R2dbcDatabase) {
    suspendTransaction(db = database) {
        SchemaUtils.drop(*R2dbcBenchmarkInfra.ALL_TABLES)
        SchemaUtils.create(*R2dbcBenchmarkInfra.ALL_TABLES)
    }
}

internal suspend fun truncateWorkingTables(database: R2dbcDatabase) {
    suspendTransaction(db = database) {
        SchemaUtils.drop(BatchJobExecutionTable, BatchStepExecutionTable, BenchmarkTargetTable)
        SchemaUtils.create(BatchJobExecutionTable, BatchStepExecutionTable, BenchmarkTargetTable)
    }
}

internal suspend fun seedSourceRows(database: R2dbcDatabase, dataSize: Int) {
    dataSize.requirePositiveNumber("dataSize")
    (1..dataSize).chunked(R2dbcBenchmarkInfra.INSERT_CHUNK_SIZE).forEach { chunk ->
        suspendTransaction(db = database) {
            BenchmarkSourceTable.batchInsert(chunk, shouldReturnGeneratedValues = false) { value ->
                this[BenchmarkSourceTable.name] = "item-$value"
                this[BenchmarkSourceTable.value] = value
            }
        }
    }
}

internal suspend fun minMaxKey(database: R2dbcDatabase): Pair<Long, Long> {
    val minExpr = BenchmarkSourceTable.id.min()
    val maxExpr = BenchmarkSourceTable.id.max()
    val row = suspendTransaction(db = database) {
        BenchmarkSourceTable.select(minExpr, maxExpr).toList().single()
    }
    val min = row[minExpr] ?: error("소스 테이블이 비어 있습니다")
    val max = row[maxExpr] ?: error("소스 테이블이 비어 있습니다")
    return min to max
}

internal suspend fun partitionRanges(database: R2dbcDatabase, parallelism: Int): List<KeyRange> {
    parallelism.requirePositiveNumber("parallelism")
    val (minKey, maxKey) = minMaxKey(database)
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

internal suspend fun runJobPartition(
    database: R2dbcDatabase,
    jobName: String,
    minKey: Long? = null,
    maxKey: Long? = null,
): Int {
    val job = batchJob(jobName) {
        repository(ExposedR2dbcBatchJobRepository(database, CheckpointJson.jackson3()))
        step<BenchmarkSourceRecord, BenchmarkTargetRecord>("readAndWrite") {
            reader(buildReader(database, minKey, maxKey))
            processor { source -> BenchmarkTargetRecord(source.name.uppercase(), source.value * 2) }
            writer(buildWriter(database))
            chunkSize(R2dbcBenchmarkInfra.CHUNK_SIZE)
        }
    }
    return try {
        val report = job.run()
        report.stepReports.sumOf { it.writeCount.toInt() }
    } catch (e: Exception) {
        AbstractR2dbcJobState.log.error(e) {
            "배치 잡 실행 실패: jobName=$jobName, minKey=$minKey, maxKey=$maxKey"
        }
        throw e
    }
}

internal fun runParallelJobs(database: R2dbcDatabase, parallelism: Int): Int = runBlocking {
    val ranges = partitionRanges(database, parallelism)
    coroutineScope {
        ranges.mapIndexed { index, range ->
            async {
                runJobPartition(
                    database = database,
                    jobName = "r2dbcBenchmarkJob-partition-$index",
                    minKey = range.minKeyExclusive,
                    maxKey = range.maxKeyInclusive,
                )
            }
        }.awaitAll().sum()
    }
}

private fun buildReader(
    database: R2dbcDatabase,
    minKey: Long? = null,
    maxKey: Long? = null,
): ExposedR2dbcBatchReader<Long, BenchmarkSourceRecord> =
    ExposedR2dbcBatchReader(
        database = database,
        table = BenchmarkSourceTable,
        keyColumn = BenchmarkSourceTable.id,
        pageSize = R2dbcBenchmarkInfra.CHUNK_SIZE,
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

private fun buildWriter(database: R2dbcDatabase): ExposedR2dbcBatchWriter<BenchmarkTargetRecord> =
    ExposedR2dbcBatchWriter(database, BenchmarkTargetTable) { record ->
        this[BenchmarkTargetTable.sourceName] = record.sourceName
        this[BenchmarkTargetTable.transformedValue] = record.transformedValue
    }

abstract class AbstractR2dbcSeedState {

    companion object : KLoggingChannel()

    abstract val database: BenchmarkDatabase
    abstract val dataSize: Int
    abstract val poolSize: Int

    protected abstract fun connectionUrl(): String
    protected open fun databaseConfig(): R2dbcDatabaseConfig.Builder.() -> Unit = {}

    private var pool: ConnectionPool? = null
    internal var exposedDatabase: R2dbcDatabase? = null

    protected fun trialSetup() {
        log.info { "[${database.displayName}] Seed Trial setup 시작: poolSize=$poolSize" }
        val (newPool, newDatabase) = buildPoolAndDatabase(
            url = connectionUrl(),
            poolSize = poolSize,
            initialSize = minOf(2, poolSize),
            configure = databaseConfig(),
        )
        pool = newPool
        exposedDatabase = newDatabase
        runBlocking {
            resetSchema(newDatabase)
        }
        log.info { "[${database.displayName}] Seed Trial setup 완료" }
    }

    protected fun iterationSetup() {
        try {
            runBlocking {
                resetSchema(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" })
            }
        } catch (e: Exception) {
            log.error(e) { "[${database.displayName}] Seed Iteration setup 실패" }
            throw e
        }
    }

    protected fun trialTearDown() {
        runBlocking {
            runCatching {
                exposedDatabase?.let { resetSchema(it) }
            }.onFailure { e ->
                log.error(e) { "[${database.displayName}] Seed Trial teardown 중 스키마 초기화 실패" }
            }
            runCatching { pool?.close() }.onFailure { e ->
                log.error(e) { "[${database.displayName}] Seed Trial teardown 중 커넥션 풀 종료 실패" }
            }
        }
        log.info { "[${database.displayName}] Seed Trial teardown 완료" }
    }

    fun insertSourceRows(): Int {
        runBlocking {
            seedSourceRows(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" }, dataSize)
        }
        return dataSize
    }
}

abstract class AbstractR2dbcJobState {

    companion object : KLoggingChannel()

    abstract val database: BenchmarkDatabase
    abstract val dataSize: Int
    abstract val poolSize: Int
    abstract val parallelism: Int

    protected abstract fun connectionUrl(): String
    protected open fun databaseConfig(): R2dbcDatabaseConfig.Builder.() -> Unit = {}

    private var pool: ConnectionPool? = null
    internal var exposedDatabase: R2dbcDatabase? = null

    protected fun trialSetup() {
        log.info { "[${database.displayName}] Job Trial setup 시작: poolSize=$poolSize, dataSize=$dataSize" }
        val (newPool, newDatabase) = buildPoolAndDatabase(
            url = connectionUrl(),
            poolSize = poolSize,
            initialSize = minOf(2, poolSize),
            configure = databaseConfig(),
        )
        pool = newPool
        exposedDatabase = newDatabase
        runBlocking {
            resetSchema(newDatabase)
            seedSourceRows(newDatabase, dataSize)
        }
        log.info { "[${database.displayName}] Job Trial setup 완료: ${dataSize}건 적재 완료" }
    }

    protected fun iterationSetup() {
        try {
            runBlocking {
                truncateWorkingTables(requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" })
            }
        } catch (e: Exception) {
            log.error(e) { "[${database.displayName}] Job Iteration setup 실패" }
            throw e
        }
    }

    protected fun trialTearDown() {
        runBlocking {
            runCatching {
                exposedDatabase?.let { resetSchema(it) }
            }.onFailure { e ->
                log.error(e) { "[${database.displayName}] Job Trial teardown 중 스키마 초기화 실패" }
            }
            runCatching { pool?.close() }.onFailure { e ->
                log.error(e) { "[${database.displayName}] Job Trial teardown 중 커넥션 풀 종료 실패" }
            }
        }
        log.info { "[${database.displayName}] Job Trial teardown 완료" }
    }

    fun runJob(): Int {
        val database = requireNotNull(exposedDatabase) { "exposedDatabase가 초기화되지 않았습니다" }
        return if (parallelism <= 1) {
            runBlocking {
                runJobPartition(database, "r2dbcBenchmarkJob")
            }
        } else {
            runParallelJobs(database, parallelism)
        }
    }
}

internal fun postgreSqlR2dbcUrl(): String {
    val options = "?lc_messages=en_US.UTF-8"
    return if (TestDBConfig.useTestcontainers) {
        "r2dbc:postgresql://test:test@127.0.0.1:${Containers.Postgres.port}/postgres$options"
    } else {
        "r2dbc:postgresql://localhost:5432/exposed$options"
    }
}

internal fun mySqlR2dbcUrl(): String {
    val password = System.getenv("MYSQL_LOCAL_PASSWORD")
        ?: System.getProperty("mysql.local.password", "")
    if (!TestDBConfig.useTestcontainers && password.isEmpty()) {
        AbstractR2dbcJobState.log.error { "MYSQL_LOCAL_PASSWORD 환경변수와 mysql.local.password 시스템 프로퍼티가 모두 비어 있습니다." }
    }
    val options = "?useSSL=false" +
        "&characterEncoding=UTF-8" +
        "&zeroDateTimeBehavior=convertToNull" +
        "&useLegacyDatetimeCode=false" +
        "&serverTimezone=UTC" +
        "&allowPublicKeyRetrieval=true"
    return if (TestDBConfig.useTestcontainers) {
        "r2dbc:mysql://test:test@127.0.0.1:${Containers.MySQL8.port}/${Containers.MySQL8.databaseName}$options"
    } else {
        "r2dbc:mysql://exposed:$password@localhost:3306/exposed$options"
    }
}
