package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class RepeatWorkFlowTest : AbstractWorkflowTest() {

    @Test
    fun `repeatWhile 성공 조건 - N회 반복 후 종료`() {
        val counter = AtomicInteger(0)
        val flow = RepeatWorkFlow(
            work = Work("count-work") { ctx ->
                val count = counter.incrementAndGet()
                ctx["count"] = count
                WorkReport.success(ctx)
            },
            repeatPredicate = { report -> report.context.get<Int>("count")!! < 3 },
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `maxIterations 제한으로 중단`() {
        val counter = AtomicInteger(0)
        val flow = RepeatWorkFlow(
            work = Work("infinite-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            repeatPredicate = { true },  // 항상 계속 반복
            maxIterations = 5,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 5
    }

    @Test
    fun `until 조건 충족 시 중단`() {
        val counter = AtomicInteger(0)
        // until은 DSL에서만 제공하므로, RepeatWorkFlow에 반전 predicate 직접 전달
        val flow = RepeatWorkFlow(
            work = Work("count-work") { ctx ->
                val count = counter.incrementAndGet()
                ctx["done"] = count >= 4
                WorkReport.success(ctx)
            },
            repeatPredicate = { report -> report.context.get<Boolean>("done") != true },
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 4
    }

    @Test
    fun `work가 ABORTED 반환 시 즉시 중단`() {
        val counter = AtomicInteger(0)
        val flow = RepeatWorkFlow(
            work = Work("abort-after-2") { ctx ->
                val count = counter.incrementAndGet()
                if (count >= 2) WorkReport.aborted(ctx, "강제 중단")
                else WorkReport.success(ctx)
            },
            repeatPredicate = { true },
            maxIterations = 100,
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `work가 CANCELLED 반환 시 즉시 중단`() {
        val counter = AtomicInteger(0)
        val flow = RepeatWorkFlow(
            work = Work("cancel-after-3") { ctx ->
                val count = counter.incrementAndGet()
                if (count >= 3) WorkReport.cancelled(ctx, "취소")
                else WorkReport.success(ctx)
            },
            repeatPredicate = { true },
            maxIterations = 100,
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
        counter.get() shouldBeEqualTo 3
    }
}
