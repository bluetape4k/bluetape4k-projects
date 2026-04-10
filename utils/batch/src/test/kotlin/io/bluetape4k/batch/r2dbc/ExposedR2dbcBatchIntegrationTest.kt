package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.batch.SourceRecord
import io.bluetape4k.batch.TargetRecord
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.dsl.batchJob
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * R2DBC 배치 엔드투엔드 통합 테스트.
 *
 * [ExposedR2dbcBatchJobRepository] + [ExposedR2dbcBatchReader] + [ExposedR2dbcBatchWriter]를
 * [batchJob] DSL로 조합하여 전체 파이프라인 실행을 검증한다.
 *
 * H2 / PostgreSQL / MySQL 각 방언에서:
 * 1. 정상 실행 → COMPLETED
 * 2. 빈 소스 → COMPLETED, 0건
 * 3. FAILED 재시작 → 기존 Job 재사용
 * 4. Processor skip 처리 → COMPLETED_WITH_SKIPS
 */
class ExposedR2dbcBatchIntegrationTest : AbstractBatchR2dbcTest() {

    private val allTables: Array<Table> = arrayOf(
        BatchJobExecutionTable, BatchStepExecutionTable,
        BatchSourceTable, BatchTargetTable,
    )

    private suspend fun withAllTables(testDB: TestDB, block: suspend () -> Unit) {
        withTables(testDB, *allTables) { block() }
    }

    private fun makeJob(database: R2dbcDatabase, chunkSize: Int = 10) = batchJob("integrationJob") {
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

    // ─── 1. 정상 실행 → COMPLETED ─────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `정상 실행 - 100개 레코드 전부 변환 저장 COMPLETED`(testDB: TestDB) {
        runSuspendIO {
            withAllTables(testDB) {
                val database = testDB.db!!

                // 소스 데이터 삽입
                suspendTransaction(db = database) {
                    BatchSourceTable.batchInsert((1..100).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }

                val report = makeJob(database, chunkSize = 20).run()

                report shouldBeInstanceOf BatchReport.Success::class
                report.stepReports[0].readCount shouldBeEqualTo 100L
                report.stepReports[0].writeCount shouldBeEqualTo 100L
                report.stepReports[0].skipCount shouldBeEqualTo 0L

                val count = suspendTransaction(db = database) {
                    BatchTargetTable.selectAll().count()
                }
                count shouldBeEqualTo 100L
            }
        }
    }

    // ─── 2. 빈 소스 → COMPLETED, 0건 ──────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `빈 소스 - 읽기 아이템 없음 COMPLETED 0건`(testDB: TestDB) {
        runSuspendIO {
            withAllTables(testDB) {
                val database = testDB.db!!
                val report = makeJob(database).run()

                report shouldBeInstanceOf BatchReport.Success::class
                report.stepReports[0].writeCount shouldBeEqualTo 0L
            }
        }
    }

    // ─── 3. FAILED 재시작 → 기존 Job 재사용 ──────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `FAILED 재시작 - 동일 JobExecution 재사용 완료`(testDB: TestDB) {
        runSuspendIO {
            withAllTables(testDB) {
                val database = testDB.db!!

                suspendTransaction(db = database) {
                    BatchSourceTable.batchInsert((1..50).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }

                val repo = ExposedR2dbcBatchJobRepository(database, CheckpointJson.jackson3())

                // 1차 실행 후 FAILED 시뮬레이션
                val je = repo.findOrCreateJobExecution("integrationJob", emptyMap())
                repo.completeJobExecution(je, BatchStatus.FAILED)

                // 2차 실행 — FAILED JobExecution 재사용
                val report = makeJob(database, chunkSize = 10).run()

                report shouldBeInstanceOf BatchReport.Success::class

                val count = suspendTransaction(db = database) {
                    BatchTargetTable.selectAll().count()
                }
                count shouldBeEqualTo 50L
            }
        }
    }

    // ─── 4. skip 있음 → COMPLETED_WITH_SKIPS ────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `skip 있음 - 일부 아이템 skip → COMPLETED_WITH_SKIPS`(testDB: TestDB) {
        runSuspendIO {
            withAllTables(testDB) {
                val database = testDB.db!!

                suspendTransaction(db = database) {
                    BatchSourceTable.batchInsert((1..10).toList()) { i ->
                        this[BatchSourceTable.name] = "item-$i"
                        this[BatchSourceTable.value] = i
                    }
                }

                val repo = ExposedR2dbcBatchJobRepository(database, CheckpointJson.jackson3())

                val job = batchJob("skipJob") {
                    repository(repo)
                    step<SourceRecord, TargetRecord>("readAndWrite") {
                        reader(ExposedR2dbcBatchReader(
                            database = database,
                            table = BatchSourceTable,
                            keyColumn = BatchSourceTable.id,
                            pageSize = 10,
                            rowMapper = { row ->
                                SourceRecord(
                                    id = row[BatchSourceTable.id],
                                    name = row[BatchSourceTable.name],
                                    value = row[BatchSourceTable.value],
                                )
                            },
                            keyExtractor = { it.id },
                        ))
                        // value가 짝수인 아이템 processor에서 예외 → skip
                        processor { src ->
                            if (src.value % 2 == 0) throw IllegalArgumentException("even skip")
                            TargetRecord(src.name, src.value)
                        }
                        writer(ExposedR2dbcBatchWriter(database, BatchTargetTable) { record ->
                            this[BatchTargetTable.sourceName] = record.sourceName
                            this[BatchTargetTable.transformedValue] = record.transformedValue
                        })
                        chunkSize(10)
                        skipPolicy(SkipPolicy.ALL)
                    }
                }

                val report = job.run()

                report shouldBeInstanceOf BatchReport.PartiallyCompleted::class
                val stepReport = report.stepReports[0]
                stepReport.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
                stepReport.skipCount shouldBeEqualTo 5L   // 짝수 5개 skip
                stepReport.writeCount shouldBeEqualTo 5L  // 홀수 5개 저장
            }
        }
    }
}
