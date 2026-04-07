package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class WorkReportFlowTest: AbstractWorkflowTest() {

    @Test
    fun `workReportFlow 순차 수집`() = runTest {
        val works = listOf(successSuspendWork(), successSuspendWork(), failSuspendWork())
        val reports = workReportFlow(works, context).toList()

        reports shouldHaveSize 3
        reports[0].isSuccess.shouldBeTrue()
        reports[1].isSuccess.shouldBeTrue()
        reports[2].isFailure.shouldBeTrue()
    }

    @Test
    fun `workReportFlow 전체 성공`() = runTest {
        val works = listOf(
            successSuspendWork("work-1"),
            successSuspendWork("work-2"),
            successSuspendWork("work-3"),
        )
        val reports = workReportFlow(works, context).toList()

        reports shouldHaveSize 3
        reports.all { it.isSuccess }.shouldBeTrue()
    }

    @Test
    fun `workReportFlow 빈 목록 - 빈 Flow`() = runTest {
        val reports = workReportFlow(emptyList(), context).toList()

        reports shouldHaveSize 0
    }

    @Test
    fun `executeAsFlow 단일 결과`() = runTest {
        val report = successSuspendWork().executeAsFlow(context).single()

        report.isSuccess.shouldBeTrue()
        report is WorkReport.Success
    }

    @Test
    fun `executeAsFlow 실패 작업`() = runTest {
        val report = failSuspendWork().executeAsFlow(context).single()

        report.isFailure.shouldBeTrue()
        report is WorkReport.Failure
    }

    @Test
    fun `executeAsFlow abort 작업`() = runTest {
        val report = abortSuspendWork().executeAsFlow(context).single()

        report.isAborted.shouldBeTrue()
        report is WorkReport.Aborted
    }

    @Test
    fun `workReportFlow aborted 포함 - 모든 결과 수집`() = runTest {
        val works = listOf(
            successSuspendWork("work-1"),
            abortSuspendWork("abort-work"),
            successSuspendWork("work-3"),
        )
        val reports = workReportFlow(works, context).toList()

        // workReportFlow는 각 작업 결과를 순차 emit (중단 없음)
        reports shouldHaveSize 3
        reports[0].isSuccess.shouldBeTrue()
        reports[1].isAborted.shouldBeTrue()
        reports[2].isSuccess.shouldBeTrue()
    }
}
