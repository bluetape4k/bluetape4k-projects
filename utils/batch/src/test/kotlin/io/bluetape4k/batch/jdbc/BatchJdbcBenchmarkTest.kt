package io.bluetape4k.batch.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.SourceRecord
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.core.InMemoryBatchJobRepository
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.system.measureTimeMillis

/**
 * JDBC 배치 성능 측정용 레거시 테스트입니다.
 *
 * DB 종류 (H2 / PostgreSQL / MySQL) × 데이터 사이즈 (소 100 / 중 10,000 / 대 100,000) 조합으로
 * 배치 파이프라인의 읽기·쓰기 처리량을 측정합니다.
 * 현재 공식 benchmark는 `src/benchmark`의 `kotlinx-benchmark` 기반 구현과
 * `docs/benchmark/database_name.md` 문서를 기준으로 관리하며, 이 테스트는 기존 비교 방식의 참고용 시나리오를 유지합니다.
 *
 * **HikariCP 커넥션 풀**을 사용하여 R2DBC (`r2dbc-pool`) 측정과 동일한 조건을 맞춥니다.
 *
 * 측정 항목:
 * - 소스 데이터 적재 시간 (`batchInsert`)
 * - 배치 Job 전체 실행 시간 (Reader + Processor + Writer)
 * - 처리량 (items/sec)
 */
class BatchJdbcBenchmarkTest : AbstractBatchJdbcTest() {

    companion object : KLogging() {
        /** 데이터 사이즈 정의 (소/중/대) */
        private val DATA_SIZES = listOf(
            "소" to 100,
            "중" to 10_000,
            "대" to 100_000,
        )

        /**
         * TestDB × DataSize 조합 파라미터 소스.
         *
         * 활성화된 dialect × 3가지 데이터 사이즈 조합을 반환한다.
         */
        @JvmStatic
        fun benchmarkCombinations(): List<Array<Any>> =
            enableDialects().flatMap { testDB ->
                DATA_SIZES.map { (label, size) ->
                    arrayOf(testDB, label, size)
                }
            }
    }

    private val allTables: Array<Table> = arrayOf(
        BatchJobExecutionTable, BatchStepExecutionTable,
        BatchSourceTable, BatchTargetTable,
    )

    /**
     * JDBC batchInsert로 소스 데이터를 일괄 적재한다.
     *
     * @param database HikariCP 풀이 연결된 Exposed [Database]
     * @param count 삽입할 레코드 수
     * @return 소요 시간(ms)
     */
    private fun insertSourceData(database: Database, count: Int): Long = measureTimeMillis {
        // 대용량 INSERT는 청크 단위로 나눠 OOM 방지
        val chunkSize = 1_000
        (1..count).chunked(chunkSize).forEach { chunk ->
            transaction(database) {
                BatchSourceTable.batchInsert(chunk) { i ->
                    this[BatchSourceTable.name] = "item-$i"
                    this[BatchSourceTable.value] = i
                }
            }
        }
    }

    /**
     * 배치 Job을 생성한다.
     *
     * @param database HikariCP 풀이 연결된 Exposed [Database]
     * @param chunkSize 청크 크기 (기본값: 500)
     */
    private fun makeJob(database: Database, chunkSize: Int = 500) = batchJob("benchmarkJob") {
        repository(ExposedJdbcBatchJobRepository(database, CheckpointJson.jackson3()))
        step<SourceRecord, TargetRecord>("readAndWrite") {
            reader(ExposedJdbcBatchReader(
                database = database,
                table = BatchSourceTable,
                keyColumn = BatchSourceTable.id,
                pageSize = chunkSize,
                rowMapper = { row ->
                    SourceRecord(
                        id = row[BatchSourceTable.id],
                        name = row[BatchSourceTable.name],
                        value = row[BatchSourceTable.value],
                    )
                },
                keyExtractor = { it.id },
            ))
            processor { src -> TargetRecord(src.name.uppercase(), src.value * 2) }
            writer(ExposedJdbcBatchWriter(database, BatchTargetTable) { record ->
                this[BatchTargetTable.sourceName] = record.sourceName
                this[BatchTargetTable.transformedValue] = record.transformedValue
            })
            chunkSize(chunkSize)
        }
    }

    /**
     * 파티션 범위가 지정된 배치 Job을 생성한다 (병렬 처리용).
     *
     * @param database HikariCP 풀이 연결된 Exposed [Database]
     * @param minKey 파티션 시작 키 (exclusive)
     * @param maxKey 파티션 종료 키 (inclusive)
     * @param chunkSize 청크 크기 (기본값: 500)
     */
    private fun makePartitionJob(database: Database, minKey: Long, maxKey: Long, chunkSize: Int = 500) =
        batchJob("partitionJob-$minKey") {
            // 병렬 처리 시 각 파티션은 독립된 InMemoryBatchJobRepository 사용
            repository(InMemoryBatchJobRepository())
            step<SourceRecord, TargetRecord>("readAndWrite") {
                reader(ExposedJdbcBatchReader(
                    database = database,
                    table = BatchSourceTable,
                    keyColumn = BatchSourceTable.id,
                    pageSize = chunkSize,
                    rowMapper = { row ->
                        SourceRecord(
                            id = row[BatchSourceTable.id],
                            name = row[BatchSourceTable.name],
                            value = row[BatchSourceTable.value],
                        )
                    },
                    keyExtractor = { it.id },
                    minKey = minKey,
                    maxKey = maxKey,
                ))
                processor { src -> TargetRecord(src.name.uppercase(), src.value * 2) }
                writer(ExposedJdbcBatchWriter(database, BatchTargetTable) { record ->
                    this[BatchTargetTable.sourceName] = record.sourceName
                    this[BatchTargetTable.transformedValue] = record.transformedValue
                })
                chunkSize(chunkSize)
            }
        }

    /**
     * JDBC 배치 벤치마크: DB 종류 × 데이터 사이즈 조합별 처리량 측정.
     *
     * HikariCP 커넥션 풀을 직접 구성하여 r2dbc-pool 벤치마크와 공정하게 비교한다.
     *
     * @param testDB 대상 DB
     * @param sizeLabel 데이터 사이즈 레이블 (소/중/대)
     * @param dataSize 실제 레코드 수
     */
    @ParameterizedTest
    @MethodSource("benchmarkCombinations")
    fun `JDBC 배치 벤치마크 - DB 종류 x 데이터 사이즈 처리량`(
        testDB: TestDB,
        sizeLabel: String,
        dataSize: Int,
    ) {
        // HikariCP 커넥션 풀 구성 — r2dbc-pool 과 동일한 풀 크기로 공정 비교
        testDB.beforeConnection()
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = testDB.connection()
            driverClassName = testDB.driver
            if (testDB.user.isNotEmpty()) username = testDB.user
            if (testDB.pass.isNotEmpty()) password = testDB.pass
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30_000
            poolName = "bench-jdbc-${testDB.name.lowercase()}"
        })
        val database = Database.connect(dataSource)

        try {
            // 테이블 생성
            transaction(database) {
                SchemaUtils.drop(*allTables)
                SchemaUtils.create(*allTables)
            }

            val insertMs = insertSourceData(database, dataSize)
            log.info { "[JDBC-$testDB / $sizeLabel (${dataSize}건)] 삽입: ${insertMs}ms" }

            val job = makeJob(database)
            var report: BatchReport? = null
            val jobMs = measureTimeMillis {
                runSuspendIO { report = job.run() }
            }

            report shouldBeInstanceOf BatchReport.Success::class
            val stepReport = report!!.stepReports[0]

            val throughput = if (jobMs > 0) (dataSize * 1000L / jobMs) else Long.MAX_VALUE
            log.info {
                "[JDBC-$testDB / $sizeLabel (${dataSize}건)] " +
                    "Job: ${jobMs}ms, " +
                    "read=${stepReport.readCount}, write=${stepReport.writeCount}, " +
                    "처리량=${throughput}건/s"
            }
        } finally {
            runCatching { transaction(database) { SchemaUtils.drop(*allTables) } }
            runCatching { dataSource.close() }
        }
    }

    /**
     * JDBC 병렬 배치 벤치마크: 4개 파티션을 코루틴으로 동시 실행하여 처리량을 측정한다.
     *
     * 각 파티션은 독립된 [InMemoryBatchJobRepository]와 키 범위 제한([minKey]/[maxKey])을 가진 Reader를 사용한다.
     * 순차 벤치마크 결과와 비교하여 병렬화 효과를 측정한다.
     *
     * @param testDB 대상 DB
     * @param sizeLabel 데이터 사이즈 레이블 (소/중/대)
     * @param dataSize 실제 레코드 수
     */
    @ParameterizedTest
    @MethodSource("benchmarkCombinations")
    fun `JDBC 병렬 배치 벤치마크 - DB 종류 x 데이터 사이즈 처리량`(
        testDB: TestDB,
        sizeLabel: String,
        dataSize: Int,
    ) {
        val parallelism = 4
        testDB.beforeConnection()
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = testDB.connection()
            driverClassName = testDB.driver
            if (testDB.user.isNotEmpty()) username = testDB.user
            if (testDB.pass.isNotEmpty()) password = testDB.pass
            maximumPoolSize = parallelism * 3  // 파티션당 여유 있는 풀
            minimumIdle = parallelism
            connectionTimeout = 30_000
            poolName = "bench-jdbc-parallel-${testDB.name.lowercase()}"
        })
        val database = Database.connect(dataSource)

        try {
            transaction(database) {
                SchemaUtils.drop(*allTables)
                SchemaUtils.create(*allTables)
            }

            val insertMs = insertSourceData(database, dataSize)
            log.info { "[JDBC-병렬-$testDB / $sizeLabel (${dataSize}건)] 삽입: ${insertMs}ms" }

            // 파티션 경계 계산 — IDs는 1..dataSize (fresh table 기준)
            val partSize = dataSize / parallelism

            var totalWriteCount = 0L
            val jobMs = measureTimeMillis {
                runSuspendIO {
                    coroutineScope {
                        (0 until parallelism).map { i ->
                            val minKey = (i * partSize).toLong()          // exclusive
                            val maxKey = if (i == parallelism - 1) Long.MAX_VALUE
                                         else ((i + 1) * partSize).toLong()  // inclusive
                            async {
                                val job = makePartitionJob(database, minKey, maxKey)
                                job.run()
                            }
                        }.awaitAll().also { reports ->
                            totalWriteCount = reports.sumOf { it.stepReports[0].writeCount }
                        }
                    }
                }
            }

            val throughput = if (jobMs > 0) (dataSize * 1000L / jobMs) else Long.MAX_VALUE
            log.info {
                "[JDBC-병렬-$testDB / $sizeLabel (${dataSize}건)] " +
                    "Job: ${jobMs}ms, parallelism=$parallelism, " +
                    "write=${totalWriteCount}, " +
                    "처리량=${throughput}건/s"
            }
        } finally {
            runCatching { transaction(database) { SchemaUtils.drop(*allTables) } }
            runCatching { dataSource.close() }
        }
    }
}
