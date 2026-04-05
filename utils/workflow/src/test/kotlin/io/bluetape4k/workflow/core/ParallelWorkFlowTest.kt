package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ParallelWorkFlowTest : AbstractWorkflowTest() {

    @Test
    fun `전체 성공 - Success 반환`() {
        val works = listOf(
            successWork("work-1"),
            successWork("work-2"),
            successWork("work-3"),
        )
        val flow = ParallelWorkFlow(works)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `일부 실패 - Failure 반환`() {
        val works = listOf(
            successWork("work-1"),
            failWork("fail-work"),
            successWork("work-3"),
        )
        val flow = ParallelWorkFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `하나라도 ABORTED - Aborted 반환, ABORTED가 FAILED보다 우선순위 높음`() {
        val works = listOf(
            successWork("work-1"),
            failWork("fail-work"),
            abortWork("abort-work"),
            successWork("work-4"),
        )
        val flow = ParallelWorkFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }

    @Test
    fun `timeout 초과 시 미완료 태스크 Cancelled 처리`() {
        val works = listOf(
            successWork("fast-work"),
            Work("slow-work") { ctx ->
                Thread.sleep(500)  // timeout보다 긴 대기
                WorkReport.success(ctx)
            },
        )
        val flow = ParallelWorkFlow(
            works = works,
            timeout = 100.milliseconds,
        )

        val report = flow.execute(context)

        // slow-work가 타임아웃되어 Cancelled 반환
        report shouldBeInstanceOf WorkReport.Cancelled::class
    }

    @Test
    fun `빈 works - Success 반환`() {
        val flow = ParallelWorkFlow(emptyList())

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    // ──────────────────────────────────────────────────
    // ParallelPolicy.ANY 테스트
    // ──────────────────────────────────────────────────

    @Test
    fun `ANY 정책 - 첫 번째 성공 즉시 반환`() {
        val works = listOf(
            successWork("work-1"),
            successWork("work-2"),
            successWork("work-3"),
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ANY 정책 - 모두 실패하면 Failure 반환`() {
        val works = listOf(
            failWork("fail-1"),
            failWork("fail-2"),
            failWork("fail-3"),
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `ANY 정책 - 일부 실패 일부 성공이면 첫 성공 반환`() {
        val works = listOf(
            failWork("fail-1"),
            successWork("success-2"),
            failWork("fail-3"),
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ANY 정책 - 빠른 성공 작업이 느린 성공보다 먼저 반환`() {
        val works = listOf(
            delayedSuccessWork(300L, "slow-work"),
            delayedSuccessWork(10L, "fast-work"),
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ALL vs ANY 정책 비교 - 동일 works에서 결과 타입이 다름`() {
        val works = listOf(
            successWork("work-1"),
            failWork("fail-work"),
            successWork("work-3"),
        )

        val allReport = ParallelWorkFlow(works, policy = ParallelPolicy.ALL).execute(context)
        val anyReport = ParallelWorkFlow(works, policy = ParallelPolicy.ANY).execute(context)

        // ALL: 하나라도 실패 → Failure
        allReport shouldBeInstanceOf WorkReport.Failure::class
        // ANY: 성공이 하나라도 있으면 → Success
        anyReport.isSuccess.shouldBeTrue()
        anyReport shouldBeInstanceOf WorkReport.Success::class
    }
}
