package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class SequentialWorkFlowTest: AbstractWorkflowTest() {

    @Test
    fun `전체 성공 - Success 반환`() {
        val counter = AtomicInteger(0)
        val works = (1..3).map { i ->
            Work("work-$i") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            }
        }
        val flow = SequentialWorkFlow(works)

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `중간 실패 STOP - 즉시 Failure 반환`() {
        val counter = AtomicInteger(0)
        val works = listOf(
            Work("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            Work("work-2") { ctx -> counter.incrementAndGet(); WorkReport.failure(ctx, RuntimeException("실패")) },
            Work("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flow = SequentialWorkFlow(works, errorStrategy = ErrorStrategy.STOP)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `중간 실패 CONTINUE - PartialSuccess 반환하고 failedReports 누적`() {
        val works = listOf(
            Work("work-1") { ctx -> WorkReport.success(ctx) },
            Work("work-2") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-2")) },
            Work("work-3") { ctx -> WorkReport.success(ctx) },
            Work("work-4") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-4")) },
        )
        val flow = SequentialWorkFlow(works, errorStrategy = ErrorStrategy.CONTINUE)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.PartialSuccess::class
        val partial = report as WorkReport.PartialSuccess
        partial.failedReports.size shouldBeEqualTo 2
    }

    @Test
    fun `전체 실패 CONTINUE - PartialSuccess 반환, failedReports 크기 == works 크기`() {
        val works = (1..3).map { i ->
            Work("fail-$i") { ctx -> WorkReport.failure(ctx, RuntimeException("fail-$i")) }
        }
        val flow = SequentialWorkFlow(works, errorStrategy = ErrorStrategy.CONTINUE)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.PartialSuccess::class
        val partial = report as WorkReport.PartialSuccess
        partial.failedReports.size shouldBeEqualTo works.size
    }

    @Test
    fun `빈 works - Success 반환`() {
        val flow = SequentialWorkFlow(emptyList())

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
    }

    @Test
    fun `ABORTED 반환 시 ErrorStrategy 무관 즉시 Aborted 반환`() {
        val counter = AtomicInteger(0)
        val works = listOf(
            Work("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            Work("abort-work") { ctx -> counter.incrementAndGet(); WorkReport.aborted(ctx, "중단") },
            Work("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )

        // STOP 전략에서도
        val flowStop = SequentialWorkFlow(works, errorStrategy = ErrorStrategy.STOP)
        val reportStop = flowStop.execute(context)
        reportStop shouldBeInstanceOf WorkReport.Aborted::class
        counter.get() shouldBeEqualTo 2

        // CONTINUE 전략에서도 동일
        val counter2 = AtomicInteger(0)
        val works2 = listOf(
            Work("work-1") { ctx -> counter2.incrementAndGet(); WorkReport.success(ctx) },
            Work("abort-work") { ctx -> counter2.incrementAndGet(); WorkReport.aborted(ctx, "중단") },
            Work("work-3") { ctx -> counter2.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flowContinue = SequentialWorkFlow(works2, errorStrategy = ErrorStrategy.CONTINUE)
        val reportContinue = flowContinue.execute(context)
        reportContinue shouldBeInstanceOf WorkReport.Aborted::class
        counter2.get() shouldBeEqualTo 2
    }

    @Test
    fun `CANCELLED 반환 시 즉시 Cancelled 반환`() {
        val counter = AtomicInteger(0)
        val works = listOf(
            Work("work-1") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
            Work("cancel-work") { ctx -> counter.incrementAndGet(); WorkReport.cancelled(ctx, "취소") },
            Work("work-3") { ctx -> counter.incrementAndGet(); WorkReport.success(ctx) },
        )
        val flow = SequentialWorkFlow(works)

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `중첩 SequentialWorkFlow - sequential 안에 sequential 실행`() {
        val outerCounter = AtomicInteger(0)
        val innerCounter = AtomicInteger(0)

        val innerFlow = SequentialWorkFlow(
            works = listOf(
                Work("inner-1") { ctx -> innerCounter.incrementAndGet(); WorkReport.success(ctx) },
                Work("inner-2") { ctx -> innerCounter.incrementAndGet(); WorkReport.success(ctx) },
            ),
        )

        val outerFlow = SequentialWorkFlow(
            works = listOf(
                Work("outer-1") { ctx -> outerCounter.incrementAndGet(); WorkReport.success(ctx) },
                innerFlow,
                Work("outer-2") { ctx -> outerCounter.incrementAndGet(); WorkReport.success(ctx) },
            ),
        )

        val report = outerFlow.execute(context)

        report.isSuccess.shouldBeTrue()
        outerCounter.get() shouldBeEqualTo 2
        innerCounter.get() shouldBeEqualTo 2
    }
}
