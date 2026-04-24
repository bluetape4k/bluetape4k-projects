package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchReport] sealed interface 분기 검증 테스트.
 */
class BatchReportTest {

    companion object : KLogging()

    private val baseJobExecution = JobExecution(
        id = 1L,
        jobName = "testJob",
        status = BatchStatus.COMPLETED,
        startTime = Instant.now(),
    )

    private val successStepReport = StepReport(
        stepName = "step1",
        status = BatchStatus.COMPLETED,
        readCount = 100L,
        writeCount = 100L,
        skipCount = 0L,
        duration = 2.seconds,
    )

    private val skippedStepReport = StepReport(
        stepName = "step1",
        status = BatchStatus.COMPLETED_WITH_SKIPS,
        readCount = 100L,
        writeCount = 90L,
        skipCount = 10L,
        duration = 2.seconds,
    )

    private val failedStepReport = StepReport(
        stepName = "step1",
        status = BatchStatus.FAILED,
        readCount = 50L,
        writeCount = 0L,
        skipCount = 0L,
        error = RuntimeException("write failed"),
    )

    // ─── 1. BatchReport.Success ──────────────────────────────────────────────

    @Test
    fun `Success - 생성 및 기본 속성 검증`() {
        val report = BatchReport.Success(
            jobExecution = baseJobExecution,
            stepReports = listOf(successStepReport),
        )

        report.shouldNotBeNull()
        report shouldBeInstanceOf BatchReport.Success::class
        report.jobExecution shouldBeEqualTo baseJobExecution
        report.stepReports.size shouldBeEqualTo 1
        report.stepReports[0].skipCount shouldBeEqualTo 0L
    }

    @Test
    fun `Success - 빈 stepReports 허용`() {
        val report = BatchReport.Success(
            jobExecution = baseJobExecution,
            stepReports = emptyList(),
        )

        report.stepReports.size shouldBeEqualTo 0
    }

    @Test
    fun `Success - data class equality`() {
        val report1 = BatchReport.Success(baseJobExecution, listOf(successStepReport))
        val report2 = BatchReport.Success(baseJobExecution, listOf(successStepReport))

        report1 shouldBeEqualTo report2
    }

    @Test
    fun `Success - copy 동작`() {
        val original = BatchReport.Success(baseJobExecution, listOf(successStepReport))
        val copied = original.copy(stepReports = emptyList())

        copied.jobExecution shouldBeEqualTo original.jobExecution
        copied.stepReports.size shouldBeEqualTo 0
    }

    // ─── 2. BatchReport.PartiallyCompleted ──────────────────────────────────

    @Test
    fun `PartiallyCompleted - 생성 및 기본 속성 검증`() {
        val report = BatchReport.PartiallyCompleted(
            jobExecution = baseJobExecution,
            stepReports = listOf(skippedStepReport),
        )

        report.shouldNotBeNull()
        report shouldBeInstanceOf BatchReport.PartiallyCompleted::class
        report.stepReports[0].skipCount shouldBeEqualTo 10L
    }

    @Test
    fun `PartiallyCompleted - data class equality`() {
        val report1 = BatchReport.PartiallyCompleted(baseJobExecution, listOf(skippedStepReport))
        val report2 = BatchReport.PartiallyCompleted(baseJobExecution, listOf(skippedStepReport))

        report1 shouldBeEqualTo report2
    }

    // ─── 3. BatchReport.Failure ──────────────────────────────────────────────

    @Test
    fun `Failure - 생성 및 error 속성 검증`() {
        val cause = RuntimeException("batch job failed")
        val report = BatchReport.Failure(
            jobExecution = baseJobExecution,
            stepReports = listOf(failedStepReport),
            error = cause,
        )

        report.shouldNotBeNull()
        report shouldBeInstanceOf BatchReport.Failure::class
        report.error shouldBeEqualTo cause
        report.stepReports[0].status shouldBeEqualTo BatchStatus.FAILED
    }

    @Test
    fun `Failure - copy로 error 교체`() {
        val original = BatchReport.Failure(
            jobExecution = baseJobExecution,
            stepReports = listOf(failedStepReport),
            error = RuntimeException("original"),
        )
        val newCause = IllegalStateException("new error")
        val copied = original.copy(error = newCause)

        copied.error shouldBeEqualTo newCause
        copied.jobExecution shouldBeEqualTo original.jobExecution
    }

    // ─── 4. exhaustive when 분기 ────────────────────────────────────────────

    @Test
    fun `when 분기 - Success 경우 처리`() {
        val report: BatchReport = BatchReport.Success(baseJobExecution, listOf(successStepReport))

        val result = when (report) {
            is BatchReport.Success -> "success"
            is BatchReport.PartiallyCompleted -> "partial"
            is BatchReport.Failure -> "failure"
        }

        result shouldBeEqualTo "success"
    }

    @Test
    fun `when 분기 - PartiallyCompleted 경우 처리`() {
        val report: BatchReport = BatchReport.PartiallyCompleted(baseJobExecution, listOf(skippedStepReport))

        val result = when (report) {
            is BatchReport.Success -> "success"
            is BatchReport.PartiallyCompleted -> "partial"
            is BatchReport.Failure -> "failure"
        }

        result shouldBeEqualTo "partial"
    }

    @Test
    fun `when 분기 - Failure 경우 처리`() {
        val cause = RuntimeException("fail")
        val report: BatchReport = BatchReport.Failure(baseJobExecution, listOf(failedStepReport), cause)

        val result = when (report) {
            is BatchReport.Success -> "success"
            is BatchReport.PartiallyCompleted -> "partial"
            is BatchReport.Failure -> "failure: ${report.error.message}"
        }

        result shouldBeEqualTo "failure: fail"
    }

    @Test
    fun `Success는 BatchReport 타입이다`() {
        val report: BatchReport = BatchReport.Success(baseJobExecution, emptyList())
        (report is BatchReport) shouldBe true
    }

    @Test
    fun `Failure는 BatchReport 타입이다`() {
        val report: BatchReport = BatchReport.Failure(baseJobExecution, emptyList(), RuntimeException())
        (report is BatchReport) shouldBe true
    }
}
