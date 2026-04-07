package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SuspendParallelFlowTest: AbstractWorkflowTest() {

    @Test
    fun `전체 성공 - Success 반환`() = runTest {
        val counter = AtomicInteger(0)
        val works = (1..3).map { i ->
            SuspendWork("work-$i") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            }
        }
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `일부 실패 - Failure 반환`() = runTest {
        val works = listOf(
            SuspendWork("work-1") { ctx -> WorkReport.success(ctx) },
            SuspendWork("work-fail") { ctx -> WorkReport.failure(ctx, RuntimeException("병렬 실패")) },
            SuspendWork("work-3") { ctx -> WorkReport.success(ctx) },
        )
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `ABORTED 우선순위 - Aborted 반환`() = runTest {
        val works = listOf(
            SuspendWork("work-1") { ctx -> WorkReport.success(ctx) },
            SuspendWork("work-abort") { ctx -> WorkReport.aborted(ctx, "중단") },
            SuspendWork("work-fail") { ctx -> WorkReport.failure(ctx, RuntimeException("실패")) },
        )
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
    }

    @Test
    fun `CANCELLED 반환 - Cancelled 우선순위`() = runTest {
        val works = listOf(
            SuspendWork("work-1") { ctx -> WorkReport.success(ctx) },
            SuspendWork("work-cancelled") { ctx -> WorkReport.cancelled(ctx, "취소") },
            SuspendWork("work-3") { ctx -> WorkReport.success(ctx) },
        )
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
    }

    @Test
    fun `빈 works - Success 반환`() = runTest {
        val flow = SuspendParallelFlow(emptyList())

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `단일 작업 성공`() = runTest {
        val works = listOf(successSuspendWork())
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
    }

    @Test
    fun `단일 작업 실패`() = runTest {
        val works = listOf(failSuspendWork())
        val flow = SuspendParallelFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    // ──────────────────────────────────────────────────
    // ParallelPolicy.ANY 테스트
    // ──────────────────────────────────────────────────

    @Test
    fun `ANY 정책 - 첫 번째 성공 즉시 반환`() = runTest {
        val works = listOf(
            successSuspendWork("work-1"),
            successSuspendWork("work-2"),
            successSuspendWork("work-3"),
        )
        val flow = SuspendParallelFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ANY 정책 - 모두 실패하면 Failure 반환`() = runTest {
        val works = listOf(
            failSuspendWork("fail-1"),
            failSuspendWork("fail-2"),
            failSuspendWork("fail-3"),
        )
        val flow = SuspendParallelFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
    }

    @Test
    fun `ANY 정책 - 일부 실패 일부 성공이면 첫 성공 반환`() = runTest {
        val works = listOf(
            failSuspendWork("fail-1"),
            successSuspendWork("success-2"),
            failSuspendWork("fail-3"),
        )
        val flow = SuspendParallelFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ANY 정책 - 빠른 성공 작업이 느린 성공보다 먼저 반환`() = runTest {
        val works = listOf(
            delayedSuccessSuspendWork(300.milliseconds, "slow-work"),
            delayedSuccessSuspendWork(10.milliseconds, "fast-work"),
        )
        val flow = SuspendParallelFlow(works, policy = ParallelPolicy.ANY)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ALL vs ANY 정책 비교 - 동일 works에서 결과 타입이 다름`() = runTest {
        val works = listOf(
            successSuspendWork("work-1"),
            failSuspendWork("fail-work"),
            successSuspendWork("work-3"),
        )

        val allReport = SuspendParallelFlow(works, policy = ParallelPolicy.ALL).execute(context)
        val anyReport = SuspendParallelFlow(works, policy = ParallelPolicy.ANY).execute(context)

        // ALL: 하나라도 실패 → Failure
        allReport shouldBeInstanceOf WorkReport.Failure::class
        // ANY: 성공이 하나라도 있으면 → Success
        anyReport.isSuccess.shouldBeTrue()
        anyReport shouldBeInstanceOf WorkReport.Success::class
    }
}
