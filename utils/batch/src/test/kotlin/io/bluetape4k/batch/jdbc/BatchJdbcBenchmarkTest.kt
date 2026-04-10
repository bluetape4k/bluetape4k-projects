package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.SourceRecord
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.system.measureTimeMillis

/**
 * JDBC 배치 벤치마크 테스트.
 *
 * DB 종류 (H2 / PostgreSQL / MySQL) × 데이터 사이즈 (소 100 / 중 10,000 / 대 100,000) 조합으로
 * 배치 파이프라인의 읽기·쓰기 처리량을 측정한다.
 *
 * 측정 항목:
 * - 소스 데이터 적재 시간 (batchInsert)
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
     * @param testDB 대상 데이터베이스
     * @param count 삽입할 레코드 수
     * @return 소요 시간(ms)
     */
    private fun insertSourceData(testDB: TestDB, count: Int): Long = measureTimeMillis {
        transaction(testDB.db!!) {
            // 대용량 INSERT는 청크 단위로 나눠 OOM 방지
            val chunkSize = 1_000
            (1..count).chunked(chunkSize).forEach { chunk ->
                BatchSourceTable.batchInsert(chunk) { i ->
                    this[BatchSourceTable.name] = "item-$i"
                    this[BatchSourceTable.value] = i
                }
            }
        }
    }

    /**
     * 배치 Job을 실행하고 소요 시간을 반환한다.
     */
    private fun makeJob(testDB: TestDB, chunkSize: Int = 500) = batchJob("benchmarkJob") {
        repository(ExposedJdbcBatchJobRepository(testDB.db!!, CheckpointJson.jackson3()))
        step<SourceRecord, TargetRecord>("readAndWrite") {
            reader(ExposedJdbcBatchReader(
                database = testDB.db!!,
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
            writer(ExposedJdbcBatchWriter(testDB.db!!, BatchTargetTable) { record ->
                this[BatchTargetTable.sourceName] = record.sourceName
                this[BatchTargetTable.transformedValue] = record.transformedValue
            })
            chunkSize(chunkSize)
        }
    }

    /**
     * JDBC 배치 벤치마크: DB 종류 × 데이터 사이즈 조합별 처리량 측정.
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
        withTables(testDB, *allTables) {
            val insertMs = insertSourceData(testDB, dataSize)
            // runSuspendIO는 Dispatchers.IO로 전환하므로 ThreadLocal 트랜잭션이 끊긴다.
            // 삽입 데이터를 커밋해야 새 트랜잭션(T2)에서 읽힌다.
            commit()
            log.info { "[JDBC-$testDB / $sizeLabel (${dataSize}건)] 삽입: ${insertMs}ms" }

            val job = makeJob(testDB)
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
        }
    }
}
