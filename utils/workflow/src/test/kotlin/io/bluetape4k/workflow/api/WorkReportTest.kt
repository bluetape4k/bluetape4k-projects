package io.bluetape4k.workflow.api

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class WorkReportTest: AbstractWorkflowTest() {

    @Test
    fun `Success 생성 및 프로퍼티`() {
        val report = WorkReport.Success(context)

        report.status shouldBeEqualTo WorkStatus.COMPLETED
        report.context shouldBeEqualTo context
        report.error.shouldBeNull()
        report.isSuccess.shouldBeTrue()
        report.isFailure.shouldBeFalse()
        report.isAborted.shouldBeFalse()
        report.isCancelled.shouldBeFalse()
    }

    @Test
    fun `Failure 생성 및 프로퍼티`() {
        val error = RuntimeException("테스트 오류")
        val report = WorkReport.Failure(context, error)

        report.status shouldBeEqualTo WorkStatus.FAILED
        report.context shouldBeEqualTo context
        report.error shouldBeEqualTo error
        report.isFailure.shouldBeTrue()
        report.isSuccess.shouldBeFalse()
        report.isAborted.shouldBeFalse()
        report.isCancelled.shouldBeFalse()
    }

    @Test
    fun `Failure error 없이 생성`() {
        val report = WorkReport.Failure(context)

        report.isFailure.shouldBeTrue()
        report.error.shouldBeNull()
    }

    @Test
    fun `PartialSuccess 생성 및 프로퍼티`() {
        val firstError = RuntimeException("첫 번째 오류")
        val secondError = RuntimeException("두 번째 오류")
        val failedReports = listOf(
            WorkReport.Failure(context, firstError),
            WorkReport.Failure(context, secondError),
        )
        val report = WorkReport.PartialSuccess(context, failedReports)

        report.status shouldBeEqualTo WorkStatus.PARTIAL
        report.context shouldBeEqualTo context
        report.failedReports shouldBeEqualTo failedReports
        // error는 failedReports.first().error
        report.error shouldBeEqualTo firstError
        report.isSuccess.shouldBeFalse()
        report.isFailure.shouldBeFalse()
        report.isAborted.shouldBeFalse()
        report.isCancelled.shouldBeFalse()
    }

    @Test
    fun `PartialSuccess failedReports 비어있으면 error null`() {
        val report = WorkReport.PartialSuccess(context, emptyList())
        report.error.shouldBeNull()
    }

    @Test
    fun `Cancelled 생성 및 프로퍼티`() {
        val report = WorkReport.Cancelled(context, "timeout 초과")

        report.status shouldBeEqualTo WorkStatus.CANCELLED
        report.context shouldBeEqualTo context
        report.reason shouldBeEqualTo "timeout 초과"
        report.error.shouldBeNull()
        report.isCancelled.shouldBeTrue()
        report.isSuccess.shouldBeFalse()
        report.isFailure.shouldBeFalse()
        report.isAborted.shouldBeFalse()
    }

    @Test
    fun `Cancelled reason 없이 생성`() {
        val report = WorkReport.Cancelled(context)

        report.isCancelled.shouldBeTrue()
        report.reason.shouldBeNull()
    }

    @Test
    fun `Aborted 생성 및 프로퍼티`() {
        val report = WorkReport.Aborted(context, "abort flag detected")

        report.status shouldBeEqualTo WorkStatus.ABORTED
        report.context shouldBeEqualTo context
        report.reason shouldBeEqualTo "abort flag detected"
        report.error.shouldBeNull()
        report.isAborted.shouldBeTrue()
        report.isSuccess.shouldBeFalse()
        report.isFailure.shouldBeFalse()
        report.isCancelled.shouldBeFalse()
    }

    @Test
    fun `Aborted reason 없이 생성`() {
        val report = WorkReport.Aborted(context)

        report.isAborted.shouldBeTrue()
        report.reason.shouldBeNull()
    }

    @Test
    fun `factory 함수 success`() {
        val report = WorkReport.success(context)

        report.shouldBeInstanceOf<WorkReport.Success>()
        report.isSuccess.shouldBeTrue()
        report.context shouldBeEqualTo context
    }

    @Test
    fun `factory 함수 failure`() {
        val error = IllegalStateException("상태 오류")
        val report = WorkReport.failure(context, error)

        report.shouldBeInstanceOf<WorkReport.Failure>()
        report.isFailure.shouldBeTrue()
        report.error shouldBeEqualTo error
    }

    @Test
    fun `factory 함수 partialSuccess`() {
        val failedReports = listOf(WorkReport.Failure(context, RuntimeException("오류")))
        val report = WorkReport.partialSuccess(context, failedReports)

        report.shouldBeInstanceOf<WorkReport.PartialSuccess>()
        report.status shouldBeEqualTo WorkStatus.PARTIAL
        report.failedReports shouldBeEqualTo failedReports
    }

    @Test
    fun `factory 함수 cancelled`() {
        val report = WorkReport.cancelled(context, "외부 취소")

        report.shouldBeInstanceOf<WorkReport.Cancelled>()
        report.isCancelled.shouldBeTrue()
        report.reason shouldBeEqualTo "외부 취소"
    }

    @Test
    fun `factory 함수 aborted`() {
        val report = WorkReport.aborted(context, "내부 중단")

        report.shouldBeInstanceOf<WorkReport.Aborted>()
        report.isAborted.shouldBeTrue()
        report.reason shouldBeEqualTo "내부 중단"
    }

    @Test
    fun `when 분기로 모든 타입 처리`() {
        val reports = listOf(
            WorkReport.success(context),
            WorkReport.failure(context, RuntimeException()),
            WorkReport.partialSuccess(context, emptyList()),
            WorkReport.cancelled(context),
            WorkReport.aborted(context),
        )

        val statuses = reports.map { report ->
            when (report) {
                is WorkReport.Success   -> WorkStatus.COMPLETED
                is WorkReport.Failure   -> WorkStatus.FAILED
                is WorkReport.PartialSuccess -> WorkStatus.PARTIAL
                is WorkReport.Cancelled -> WorkStatus.CANCELLED
                is WorkReport.Aborted   -> WorkStatus.ABORTED
            }
        }

        statuses shouldBeEqualTo listOf(
            WorkStatus.COMPLETED,
            WorkStatus.FAILED,
            WorkStatus.PARTIAL,
            WorkStatus.CANCELLED,
            WorkStatus.ABORTED,
        )
    }
}
