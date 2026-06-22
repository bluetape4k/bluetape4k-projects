package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class ParallelWorkFlowTest: AbstractWorkflowTest() {

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

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `ALL policy cancels remaining work when one branch throws`() {
        val slowStarted = CountDownLatch(1)
        val slowInterrupted = AtomicBoolean(false)
        // StructuredTaskScopeTester stress-runs independent blocks; this assertion needs
        // one workflow-owned sibling and its exact interrupt signal.
        val works = listOf(
            interruptibleSlowWork(slowStarted, slowInterrupted),
            Work("fast-fail") { ctx ->
                slowStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
                throw IllegalStateException("fail fast")
            },
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ALL)

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class

        slowInterrupted.get().shouldBeTrue()
    }

    @Test
    fun `ALL policy cancels remaining work when one branch returns Failure`() {
        val slowStarted = CountDownLatch(1)
        val slowInterrupted = AtomicBoolean(false)
        val works = listOf(
            interruptibleSlowWork(slowStarted, slowInterrupted),
            Work("fast-failure-report") { ctx ->
                slowStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
                WorkReport.failure(ctx, IllegalStateException("failure report"))
            },
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ALL)

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class

        slowInterrupted.get().shouldBeTrue()
    }

    @Test
    fun `ALL policy cancels remaining work when one branch returns Aborted`() {
        val slowStarted = CountDownLatch(1)
        val slowInterrupted = AtomicBoolean(false)
        val works = listOf(
            interruptibleSlowWork(slowStarted, slowInterrupted),
            Work("fast-aborted-report") { ctx ->
                slowStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
                WorkReport.aborted(ctx, "aborted report")
            },
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ALL)

        flow.execute(context) shouldBeInstanceOf WorkReport.Aborted::class

        slowInterrupted.get().shouldBeTrue()
    }

    @Test
    fun `ALL policy cancels remaining work when one branch returns Cancelled`() {
        val slowStarted = CountDownLatch(1)
        val slowInterrupted = AtomicBoolean(false)
        val works = listOf(
            interruptibleSlowWork(slowStarted, slowInterrupted),
            Work("fast-cancelled-report") { ctx ->
                slowStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
                WorkReport.cancelled(ctx, "cancelled report")
            },
        )
        val flow = ParallelWorkFlow(works, policy = ParallelPolicy.ALL)

        flow.execute(context) shouldBeInstanceOf WorkReport.Cancelled::class

        slowInterrupted.get().shouldBeTrue()
    }

    @Test
    fun `하나라도 ABORTED - Aborted 반환`() {
        val works = listOf(
            successWork("work-1"),
            abortWork("abort-work"),
            successWork("work-4"),
        )
        val flow = ParallelWorkFlow(works)

        flow.execute(context) shouldBeInstanceOf WorkReport.Aborted::class
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

        // slow-work가 타임아웃되어 Cancelled 반환
        flow.execute(context) shouldBeInstanceOf WorkReport.Cancelled::class
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

        flow.execute(context) shouldBeInstanceOf WorkReport.Failure::class
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
    fun `ANY 정책 - timeout 초과 시 Cancelled 반환`() {
        val works = listOf(
            delayedSuccessWork(500L, "slow-1"),
            delayedSuccessWork(500L, "slow-2"),
        )
        val flow = ParallelWorkFlow(
            works = works,
            policy = ParallelPolicy.ANY,
            timeout = 100.milliseconds,
        )

        flow.execute(context) shouldBeInstanceOf WorkReport.Cancelled::class
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

    private fun interruptibleSlowWork(
        slowStarted: CountDownLatch,
        slowInterrupted: AtomicBoolean,
    ): Work =
        Work("slow-work") { ctx ->
            slowStarted.countDown()
            try {
                Thread.sleep(5_000)
                WorkReport.success(ctx)
            } catch (e: InterruptedException) {
                slowInterrupted.set(true)
                throw e
            }
        }
}
