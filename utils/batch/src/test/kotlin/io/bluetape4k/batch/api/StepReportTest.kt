package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * [StepReport] 및 [StepExecution] 데이터 클래스 검증 테스트.
 */
class StepReportTest {

    companion object : KLogging()

    // ─── StepReport 테스트 ────────────────────────────────────────────────────

    @Test
    fun `StepReport - 기본 생성 및 속성 검증`() {
        val report = StepReport(
            stepName = "importStep",
            status = BatchStatus.COMPLETED,
            readCount = 1000L,
            writeCount = 1000L,
            skipCount = 0L,
            duration = 5.seconds,
        )

        report.stepName shouldBeEqualTo "importStep"
        report.status shouldBeEqualTo BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 1000L
        report.writeCount shouldBeEqualTo 1000L
        report.skipCount shouldBeEqualTo 0L
        report.duration shouldBeEqualTo 5.seconds
        report.checkpoint.shouldBeNull()
        report.error.shouldBeNull()
    }

    @Test
    fun `StepReport - COMPLETED_WITH_SKIPS 상태에서 skipCount 반영`() {
        val report = StepReport(
            stepName = "importStep",
            status = BatchStatus.COMPLETED_WITH_SKIPS,
            readCount = 1000L,
            writeCount = 990L,
            skipCount = 10L,
            duration = 5.seconds,
        )

        report.status shouldBeEqualTo BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo 10L
        report.writeCount shouldBeEqualTo 990L
    }

    @Test
    fun `StepReport - FAILED 상태에서 error 포함`() {
        val cause = RuntimeException("db write error")
        val report = StepReport(
            stepName = "writeStep",
            status = BatchStatus.FAILED,
            readCount = 500L,
            writeCount = 0L,
            skipCount = 0L,
            error = cause,
        )

        report.status shouldBeEqualTo BatchStatus.FAILED
        report.error.shouldNotBeNull()
        report.error!!.message shouldBeEqualTo "db write error"
    }

    @Test
    fun `StepReport - checkpoint 설정`() {
        val checkpoint = mapOf("offset" to 500L)
        val report = StepReport(
            stepName = "step1",
            status = BatchStatus.COMPLETED,
            checkpoint = checkpoint,
        )

        report.checkpoint.shouldNotBeNull()
        (report.checkpoint as Map<*, *>)["offset"] shouldBeEqualTo 500L
    }

    @Test
    fun `StepReport - data class equality`() {
        val report1 = StepReport("step1", BatchStatus.COMPLETED, 100L, 100L, 0L, 1.seconds)
        val report2 = StepReport("step1", BatchStatus.COMPLETED, 100L, 100L, 0L, 1.seconds)

        report1 shouldBeEqualTo report2
    }

    @Test
    fun `StepReport - copy로 status 변경`() {
        val original = StepReport("step1", BatchStatus.RUNNING, 50L, 0L, 0L)
        val completed = original.copy(
            status = BatchStatus.COMPLETED,
            readCount = 100L,
            writeCount = 100L,
            duration = 3.seconds,
        )

        completed.stepName shouldBeEqualTo "step1"
        completed.status shouldBeEqualTo BatchStatus.COMPLETED
        completed.readCount shouldBeEqualTo 100L
    }

    @Test
    fun `StepReport - Serializable 구현`() {
        val report = StepReport("step1", BatchStatus.COMPLETED)
        (report is java.io.Serializable) shouldBe true
    }

    @Test
    fun `StepReport - 기본값 확인 (readCount, writeCount, skipCount = 0)`() {
        val report = StepReport(stepName = "step1", status = BatchStatus.STARTING)

        report.readCount shouldBeEqualTo 0L
        report.writeCount shouldBeEqualTo 0L
        report.skipCount shouldBeEqualTo 0L
        report.duration shouldBeEqualTo kotlin.time.Duration.ZERO
    }

    // ─── StepExecution 테스트 ─────────────────────────────────────────────────

    @Test
    fun `StepExecution - 기본 생성 및 속성 검증`() {
        val exec = StepExecution(
            id = 10L,
            jobExecutionId = 1L,
            stepName = "readAndWrite",
        )

        exec.id shouldBeEqualTo 10L
        exec.jobExecutionId shouldBeEqualTo 1L
        exec.stepName shouldBeEqualTo "readAndWrite"
        exec.status shouldBeEqualTo BatchStatus.STARTING
        exec.readCount shouldBeEqualTo 0L
        exec.writeCount shouldBeEqualTo 0L
        exec.skipCount shouldBeEqualTo 0L
        exec.checkpoint.shouldBeNull()
        exec.startTime.shouldNotBeNull()
        exec.endTime.shouldBeNull()
    }

    @Test
    fun `StepExecution - COMPLETED 상태 전이`() {
        val exec = StepExecution(id = 1L, jobExecutionId = 1L, stepName = "step1")
        val completed = exec.copy(
            status = BatchStatus.COMPLETED,
            readCount = 200L,
            writeCount = 200L,
            endTime = Instant.now(),
        )

        completed.status shouldBeEqualTo BatchStatus.COMPLETED
        completed.readCount shouldBeEqualTo 200L
        completed.endTime.shouldNotBeNull()
    }

    @Test
    fun `StepExecution - COMPLETED 상태는 재시작 시 skip 대상`() {
        val exec = StepExecution(
            id = 1L,
            jobExecutionId = 1L,
            stepName = "step1",
            status = BatchStatus.COMPLETED,
        )

        // COMPLETED / COMPLETED_WITH_SKIPS → isTerminal = true → runner가 skip 처리
        exec.status.isTerminal shouldBe true
    }

    @Test
    fun `StepExecution - COMPLETED_WITH_SKIPS 상태도 skip 대상`() {
        val exec = StepExecution(
            id = 2L,
            jobExecutionId = 1L,
            stepName = "step1",
            status = BatchStatus.COMPLETED_WITH_SKIPS,
            skipCount = 5L,
        )

        exec.status.isTerminal shouldBe true
        exec.skipCount shouldBeEqualTo 5L
    }

    @Test
    fun `StepExecution - FAILED 상태는 재시도 후보`() {
        val exec = StepExecution(
            id = 3L,
            jobExecutionId = 1L,
            stepName = "step1",
            status = BatchStatus.FAILED,
        )

        exec.status.isTerminal shouldBe true
        exec.status shouldBeEqualTo BatchStatus.FAILED
    }

    @Test
    fun `StepExecution - data class equality`() {
        val t = Instant.now()
        val exec1 = StepExecution(1L, 1L, "step1", startTime = t)
        val exec2 = StepExecution(1L, 1L, "step1", startTime = t)

        exec1 shouldBeEqualTo exec2
    }

    @Test
    fun `StepExecution - checkpoint 저장`() {
        val checkpoint = "offset:500"
        val exec = StepExecution(
            id = 1L,
            jobExecutionId = 1L,
            stepName = "step1",
            checkpoint = checkpoint,
        )

        exec.checkpoint shouldBeEqualTo checkpoint
    }

    @Test
    fun `StepExecution - Serializable 구현`() {
        val exec = StepExecution(1L, 1L, "step1")
        (exec is java.io.Serializable) shouldBe true
    }
}
