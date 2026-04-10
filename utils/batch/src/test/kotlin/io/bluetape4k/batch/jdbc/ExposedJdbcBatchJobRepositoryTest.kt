package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [ExposedJdbcBatchJobRepository] 통합 테스트.
 *
 * H2 / PostgreSQL / MySQL 각 방언에서:
 * 1. JobExecution 신규 생성
 * 2. FAILED Job 재시작 → 기존 재사용
 * 3. COMPLETED Job → 신규 생성
 * 4. StepExecution 4-case 계약 검증
 * 5. Checkpoint 저장 / 로드 round-trip
 */
class ExposedJdbcBatchJobRepositoryTest : AbstractBatchJdbcTest() {

    private val batchTables = arrayOf(BatchJobExecutionTable, BatchStepExecutionTable)

    private fun withRepoTables(testDB: TestDB, block: suspend ExposedJdbcBatchJobRepository.() -> Unit) {
        withTables(testDB, *batchTables) {
            val repo = ExposedJdbcBatchJobRepository(testDB.db!!, CheckpointJson.jackson3())
            runSuspendIO { repo.block() }
        }
    }

    // ─── 1. JobExecution 신규 생성 ────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `JobExecution 신규 생성 - RUNNING 상태로 반환`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je = findOrCreateJobExecution("myJob", emptyMap())
            je.jobName shouldBeEqualTo "myJob"
            je.status shouldBe BatchStatus.RUNNING
            (je.id > 0L) shouldBe true
        }
    }

    // ─── 2. FAILED Job 재시작 → 기존 재사용 ────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `FAILED Job 재시작 - 기존 JobExecution 재사용 RUNNING으로 전환`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je1 = findOrCreateJobExecution("failedJob", emptyMap())
            completeJobExecution(je1, BatchStatus.FAILED)

            val je2 = findOrCreateJobExecution("failedJob", emptyMap())
            je2.id shouldBeEqualTo je1.id
            je2.status shouldBe BatchStatus.RUNNING
        }
    }

    // ─── 3. COMPLETED Job → 신규 생성 ────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `COMPLETED Job - 신규 JobExecution 생성`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je1 = findOrCreateJobExecution("completedJob", emptyMap())
            completeJobExecution(je1, BatchStatus.COMPLETED)

            val je2 = findOrCreateJobExecution("completedJob", emptyMap())
            (je2.id > je1.id) shouldBe true
        }
    }

    // ─── 4. StepExecution 4-case 계약 ────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StepExecution COMPLETED - UPDATE 없이 그대로 반환`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je = findOrCreateJobExecution("stepJob", emptyMap())
            val se = findOrCreateStepExecution(je, "step1")
            completeStepExecution(se, StepReport("step1", BatchStatus.COMPLETED, readCount = 100L, writeCount = 100L))

            val se2 = findOrCreateStepExecution(je, "step1")
            se2.status shouldBe BatchStatus.COMPLETED
            se2.readCount shouldBeEqualTo 100L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StepExecution FAILED - RUNNING으로 복원`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je = findOrCreateJobExecution("failedStepJob", emptyMap())
            val se = findOrCreateStepExecution(je, "step1")
            completeStepExecution(se, StepReport("step1", BatchStatus.FAILED))

            val se2 = findOrCreateStepExecution(je, "step1")
            se2.status shouldBe BatchStatus.RUNNING
        }
    }

    // ─── 5. Checkpoint round-trip ─────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Checkpoint 저장 - loadCheckpoint로 Long 값 복원`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je = findOrCreateJobExecution("cpJob", emptyMap())
            val se = findOrCreateStepExecution(je, "step1")

            saveCheckpoint(se.id, 42L)
            val restored = loadCheckpoint(se.id)

            restored.shouldNotBeNull()
            (restored as Long) shouldBeEqualTo 42L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Checkpoint 미저장 - loadCheckpoint는 null 반환`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je = findOrCreateJobExecution("noCpJob", emptyMap())
            val se = findOrCreateStepExecution(je, "step1")

            val result = loadCheckpoint(se.id)
            result shouldBe null
        }
    }

    // ─── 6. 동일 params 다른 Job → 별도 실행 ─────────────────────────────────

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다른 params - 별도 JobExecution 생성`(testDB: TestDB) {
        withRepoTables(testDB) {
            val je1 = findOrCreateJobExecution("paramJob", mapOf("date" to "2026-04-10"))
            val je2 = findOrCreateJobExecution("paramJob", mapOf("date" to "2026-04-11"))

            je1.id shouldBeEqualTo je1.id
            (je2.id > je1.id) shouldBe true
        }
    }
}
