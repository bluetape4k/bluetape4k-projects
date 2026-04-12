package io.bluetape4k.batch.r2dbc

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
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.system.measureTimeMillis

/**
 * R2DBC 배치 성능 측정용 레거시 테스트입니다.
 *
 * DB 종류 (H2 / PostgreSQL / MySQL) × 데이터 사이즈 (소 100 / 중 10,000 / 대 100,000) 조합으로
 * R2DBC 배치 파이프라인의 읽기·쓰기 처리량을 측정합니다.
 * 현재 공식 benchmark는 `src/benchmark`의 `kotlinx-benchmark` 기반 구현과
 * `docs/benchmark/database_name.md` 문서를 기준으로 관리하며, 이 테스트는 기존 비교 방식의 참고용 시나리오를 유지합니다.
 *
 * **r2dbc-pool 커넥션 풀**을 사용하여 JDBC (`HikariCP`) 측정과 동일한 조건을 맞춥니다.
 *
 * 측정 항목:
 * - 소스 데이터 적재 시간 (`R2DBC batchInsert`)
 * - 배치 Job 전체 실행 시간 (Reader + Processor + Writer)
 * - 처리량 (items/sec)
 */
class BatchR2dbcBenchmarkTest : AbstractBatchR2dbcTest() {

    companion object : KLoggingChannel() {
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
     * R2DBC batchInsert로 소스 데이터를 일괄 적재한다.
     *
     * @param database r2dbc-pool이 연결된 [R2dbcDatabase]
     * @param count 삽입할 레코드 수
     * @return 소요 시간(ms)
     */
    private suspend fun insertSourceData(database: R2dbcDatabase, count: Int): Long {
        var elapsed = 0L
        elapsed = measureTimeMillis {
            // 대용량 INSERT는 청크 단위로 나눠 OOM 방지
            val chunkSize = 1_000
            (1..count).chunked(chunkSize).forEach { chunk ->
                suspendTransaction(db = database) {
                    BatchSourceTable.batchInsert(chunk) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }
            }
        }
        return elapsed
    }

    /**
     * 배치 Job을 생성한다.
     *
     * @param database r2dbc-pool이 연결된 [R2dbcDatabase]
     * @param chunkSize 청크 크기 (기본값: 500)
     */
    private fun makeJob(database: R2dbcDatabase, chunkSize: Int = 500) = batchJob("benchmarkJob") {
        repository(ExposedR2dbcBatchJobRepository(database, CheckpointJson.jackson3()))
        step<SourceRecord, TargetRecord>("readAndWrite") {
            reader(ExposedR2dbcBatchReader(
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
            writer(ExposedR2dbcBatchWriter(database, BatchTargetTable) { record ->
                this[BatchTargetTable.sourceName] = record.sourceName
                this[BatchTargetTable.transformedValue] = record.transformedValue
            })
            chunkSize(chunkSize)
        }
    }

    /**
     * 파티션 범위가 지정된 R2DBC 배치 Job을 생성한다 (병렬 처리용).
     *
     * @param database r2dbc-pool이 연결된 [R2dbcDatabase]
     * @param minKey 파티션 시작 키 (exclusive)
     * @param maxKey 파티션 종료 키 (inclusive)
     * @param chunkSize 청크 크기 (기본값: 500)
     */
    private fun makePartitionJob(database: R2dbcDatabase, minKey: Long, maxKey: Long, chunkSize: Int = 500) =
        batchJob("partitionJob-$minKey") {
            repository(InMemoryBatchJobRepository())
            step<SourceRecord, TargetRecord>("readAndWrite") {
                reader(ExposedR2dbcBatchReader(
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
                writer(ExposedR2dbcBatchWriter(database, BatchTargetTable) { record ->
                    this[BatchTargetTable.sourceName] = record.sourceName
                    this[BatchTargetTable.transformedValue] = record.transformedValue
                })
                chunkSize(chunkSize)
            }
        }

    /**
     * R2DBC 배치 벤치마크: DB 종류 × 데이터 사이즈 조합별 처리량 측정.
     *
     * r2dbc-pool 커넥션 풀을 직접 구성하여 HikariCP JDBC 벤치마크와 공정하게 비교한다.
     *
     * @param testDB 대상 DB
     * @param sizeLabel 데이터 사이즈 레이블 (소/중/대)
     * @param dataSize 실제 레코드 수
     */
    @ParameterizedTest
    @MethodSource("benchmarkCombinations")
    fun `R2DBC 배치 벤치마크 - DB 종류 x 데이터 사이즈 처리량`(
        testDB: TestDB,
        sizeLabel: String,
        dataSize: Int,
    ) {
        runSuspendIO {
            // r2dbc-pool 커넥션 풀 구성 — HikariCP 와 동일한 풀 크기로 공정 비교
            testDB.beforeConnection()
            val url = testDB.connection()
            val options = ConnectionFactoryOptions.parse(url)
            val connectionFactory = ConnectionFactories.get(options)
            val pool = ConnectionPool(
                ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxSize(10)
                    .initialSize(2)
                    .build()
            )
            val config = R2dbcDatabaseConfig {
                this.connectionFactoryOptions = options
                testDB.dbConfig.invoke(this)
            }
            val database = R2dbcDatabase.connect(pool, databaseConfig = config)

            try {
                // 테이블 생성
                suspendTransaction(db = database) {
                    SchemaUtils.drop(*allTables)
                    SchemaUtils.create(*allTables)
                }

                val insertMs = insertSourceData(database, dataSize)
                log.info { "[R2DBC-$testDB / $sizeLabel (${dataSize}건)] 삽입: ${insertMs}ms" }

                val job = makeJob(database)
                var report: BatchReport? = null
                val jobMs = measureTimeMillis {
                    report = job.run()
                }

                report shouldBeInstanceOf BatchReport.Success::class
                val stepReport = report!!.stepReports[0]

                val throughput = if (jobMs > 0) (dataSize * 1000L / jobMs) else Long.MAX_VALUE
                log.info {
                    "[R2DBC-$testDB / $sizeLabel (${dataSize}건)] " +
                        "Job: ${jobMs}ms, " +
                        "read=${stepReport.readCount}, write=${stepReport.writeCount}, " +
                        "처리량=${throughput}건/s"
                }
            } finally {
                runCatching { suspendTransaction(db = database) { SchemaUtils.drop(*allTables) } }
                runCatching { pool.close() }
            }
        }
    }

    /**
     * R2DBC 병렬 배치 벤치마크: 4개 파티션을 코루틴으로 동시 실행하여 처리량을 측정한다.
     *
     * 각 파티션은 독립된 [InMemoryBatchJobRepository]와 키 범위 제한([minKey]/[maxKey])을 가진 Reader를 사용한다.
     * 순차 벤치마크 결과와 비교하여 R2DBC 비동기 병렬화 효과를 측정한다.
     *
     * @param testDB 대상 DB
     * @param sizeLabel 데이터 사이즈 레이블 (소/중/대)
     * @param dataSize 실제 레코드 수
     */
    @ParameterizedTest
    @MethodSource("benchmarkCombinations")
    fun `R2DBC 병렬 배치 벤치마크 - DB 종류 x 데이터 사이즈 처리량`(
        testDB: TestDB,
        sizeLabel: String,
        dataSize: Int,
    ) {
        runSuspendIO {
            val parallelism = 4
            testDB.beforeConnection()
            val url = testDB.connection()
            val options = ConnectionFactoryOptions.parse(url)
            val connectionFactory = ConnectionFactories.get(options)
            val pool = ConnectionPool(
                ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxSize(parallelism * 3)  // 파티션당 여유 있는 풀
                    .initialSize(parallelism)
                    .build()
            )
            val config = R2dbcDatabaseConfig {
                this.connectionFactoryOptions = options
                testDB.dbConfig.invoke(this)
            }
            val database = R2dbcDatabase.connect(pool, databaseConfig = config)

            try {
                suspendTransaction(db = database) {
                    SchemaUtils.drop(*allTables)
                    SchemaUtils.create(*allTables)
                }

                val insertMs = insertSourceData(database, dataSize)
                log.info { "[R2DBC-병렬-$testDB / $sizeLabel (${dataSize}건)] 삽입: ${insertMs}ms" }

                // 파티션 경계 계산 — IDs는 1..dataSize (fresh table 기준)
                val partSize = dataSize / parallelism

                var totalWriteCount = 0L
                val jobMs = measureTimeMillis {
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

                val throughput = if (jobMs > 0) (dataSize * 1000L / jobMs) else Long.MAX_VALUE
                log.info {
                    "[R2DBC-병렬-$testDB / $sizeLabel (${dataSize}건)] " +
                        "Job: ${jobMs}ms, parallelism=$parallelism, " +
                        "write=${totalWriteCount}, " +
                        "처리량=${throughput}건/s"
                }
            } finally {
                runCatching { suspendTransaction(db = database) { SchemaUtils.drop(*allTables) } }
                runCatching { pool.close() }
            }
        }
    }
}
