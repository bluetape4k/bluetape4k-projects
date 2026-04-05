package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SuspendRepeatFlowTest : AbstractWorkflowTest() {

    @Test
    fun `maxIterations 횟수만큼 반복 후 Success 반환`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("count-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            repeatPredicate = { it.isSuccess },
            maxIterations = 5,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 5
    }

    @Test
    fun `repeatWhile false - 첫 반복 후 중단`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("count-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            repeatPredicate = { false },
            maxIterations = 10,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `작업 실패 후 repeatWhile false - 중단`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("fail-once") { ctx ->
                val cnt = counter.incrementAndGet()
                if (cnt >= 2) WorkReport.failure(ctx, RuntimeException("실패"))
                else WorkReport.success(ctx)
            },
            repeatPredicate = { it.isSuccess },
            maxIterations = 10,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 2
    }

    @Test
    fun `ABORTED 반환 시 즉시 중단`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("abort-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.aborted(ctx, "중단")
            },
            repeatPredicate = { true },
            maxIterations = 10,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `CANCELLED 반환 시 즉시 중단`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("cancel-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.cancelled(ctx, "취소")
            },
            repeatPredicate = { true },
            maxIterations = 10,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `repeatDelay 0ms - 실제 지연 없이 반복`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRepeatFlow(
            work = SuspendWork("fast-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            repeatPredicate = { it.isSuccess },
            maxIterations = 3,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `컨텍스트 기반 반복 조건 - count 가 목표치 도달 시 중단`() = runTest {
        val ctx = WorkContext()
        ctx["count"] = 0

        val flow = SuspendRepeatFlow(
            work = SuspendWork("increment") { c ->
                val current = c.get<Int>("count") ?: 0
                c["count"] = current + 1
                WorkReport.success(c)
            },
            repeatPredicate = { report -> (report.context.get<Int>("count") ?: 0) < 5 },
            maxIterations = Int.MAX_VALUE,
            repeatDelay = 0.milliseconds,
        )

        val report = flow.execute(ctx)

        report.isSuccess.shouldBeTrue()
        ctx.get<Int>("count") shouldBeEqualTo 5
    }
}
