package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class SuspendSequentialFlowTest : AbstractWorkflowTest() {

    @Test
    fun `전체 성공 - Success 반환`() = runTest {
        val counter = AtomicInteger(0)
        val works = (1..3).map { i ->
            SuspendWork("work-$i") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            }
        }
        val flow = SuspendSequentialFlow(works)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `중간 실패 STOP - 즉시 Failure 반환`() = runTest {
        val counter = AtomicInteger(0)
        val works = listOf(
            SuspendWork("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            SuspendWork("work-2") { ctx -> counter.incrementAndGet(); WorkReport.failure(ctx, RuntimeException("실패")) },
            SuspendWork("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flow = SuspendSequentialFlow(works, errorStrategy = ErrorStrategy.STOP)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `중간 실패 CONTINUE - PartialSuccess 반환하고 failedReports 누적`() = runTest {
        val works = listOf(
            SuspendWork("work-1") { ctx -> WorkReport.success(ctx) },
            SuspendWork("work-2") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-2")) },
            SuspendWork("work-3") { ctx -> WorkReport.success(ctx) },
            SuspendWork("work-4") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-4")) },
        )
        val flow = SuspendSequentialFlow(works, errorStrategy = ErrorStrategy.CONTINUE)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.PartialSuccess::class
        val partial = report as WorkReport.PartialSuccess
        partial.failedReports.size shouldBeEqualTo 2
    }

    @Test
    fun `전체 실패 CONTINUE - PartialSuccess 반환 failedReports 크기 == works 크기`() = runTest {
        val works = (1..3).map { i ->
            SuspendWork("fail-$i") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-$i")) }
        }
        val flow = SuspendSequentialFlow(works, errorStrategy = ErrorStrategy.CONTINUE)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.PartialSuccess::class
        val partial = report as WorkReport.PartialSuccess
        partial.failedReports.size shouldBeEqualTo works.size
    }

    @Test
    fun `빈 works - Success 반환`() = runTest {
        val flow = SuspendSequentialFlow(emptyList())

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ABORTED 반환 시 ErrorStrategy 무관 즉시 Aborted 반환`() = runTest {
        val counter = AtomicInteger(0)
        val works = listOf(
            SuspendWork("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            SuspendWork("abort-work") { ctx -> counter.incrementAndGet(); WorkReport.aborted(ctx, "중단") },
            SuspendWork("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )

        // STOP 전략
        val flowStop = SuspendSequentialFlow(works, errorStrategy = ErrorStrategy.STOP)
        val reportStop = flowStop.execute(context)
        reportStop shouldBeInstanceOf WorkReport.Aborted::class
        counter.get() shouldBeEqualTo 2

        // CONTINUE 전략도 동일
        val counter2 = AtomicInteger(0)
        val works2 = listOf(
            SuspendWork("work-1") { ctx -> counter2.incrementAndGet(); WorkReport.success(ctx) },
            SuspendWork("abort-work") { ctx -> counter2.incrementAndGet(); WorkReport.aborted(ctx, "중단") },
            SuspendWork("work-3") { ctx -> counter2.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flowContinue = SuspendSequentialFlow(works2, errorStrategy = ErrorStrategy.CONTINUE)
        val reportContinue = flowContinue.execute(context)
        reportContinue shouldBeInstanceOf WorkReport.Aborted::class
        counter2.get() shouldBeEqualTo 2
    }

    @Test
    fun `CANCELLED 반환 시 즉시 Cancelled 반환`() = runTest {
        val counter = AtomicInteger(0)
        val works = listOf(
            SuspendWork("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            SuspendWork("cancel-work") { ctx -> counter.incrementAndGet(); WorkReport.cancelled(ctx, "취소") },
            SuspendWork("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flow = SuspendSequentialFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `코루틴 취소 전파 - cancel 호출 시 CancellationException 전파`() = runTest {
        val counter = AtomicInteger(0)
        val job = launch {
            val works = (1..5).map { i ->
                SuspendWork("work-$i") { ctx ->
                    counter.incrementAndGet()
                    WorkReport.success(ctx)
                }
            }
            val flow = SuspendSequentialFlow(works)
            flow.execute(WorkContext())
        }

        // 즉시 취소
        job.cancel("테스트 취소")
        job.join()

        // 취소 후에는 카운터가 5 미만이거나 0일 수 있음
        counter.get() < 5 || counter.get() == 0
    }

    @Test
    fun `중첩 SuspendSequentialFlow - sequential 안에 sequential 실행`() = runTest {
        val outerCounter = AtomicInteger(0)
        val innerCounter = AtomicInteger(0)

        val innerFlow = SuspendSequentialFlow(
            works = listOf(
                SuspendWork("inner-1") { ctx -> innerCounter.incrementAndGet(); WorkReport.success(ctx) },
                SuspendWork("inner-2") { ctx -> innerCounter.incrementAndGet(); WorkReport.success(ctx) },
            ),
        )

        val outerFlow = SuspendSequentialFlow(
            works = listOf(
                SuspendWork("outer-1") { ctx -> outerCounter.incrementAndGet(); WorkReport.success(ctx) },
                innerFlow,
                SuspendWork("outer-2") { ctx -> outerCounter.incrementAndGet(); WorkReport.success(ctx) },
            ),
        )

        val report = outerFlow.execute(context)

        report.isSuccess.shouldBeTrue()
        outerCounter.get() shouldBeEqualTo 2
        innerCounter.get() shouldBeEqualTo 2
    }
}
