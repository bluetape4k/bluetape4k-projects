package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SuspendRetryFlowTest: AbstractWorkflowTest() {

    @Test
    fun `첫 시도 성공 - Success 반환하고 재시도 없음`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("success-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        report shouldBeInstanceOf WorkReport.Success::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `N번 실패 후 성공 - Success 반환`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("flaky-work") { ctx ->
                val cnt = counter.incrementAndGet()
                if (cnt < 3) WorkReport.failure(ctx, RuntimeException("시도 #$cnt 실패"))
                else WorkReport.success(ctx)
            },
            retryPolicy = RetryPolicy(maxAttempts = 5, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `maxAttempts 소진 - Failure 반환`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("always-fail") { ctx ->
                counter.incrementAndGet()
                WorkReport.failure(ctx, RuntimeException("항상 실패"))
            },
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `ABORTED 반환 시 재시도 없음`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("abort-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.aborted(ctx, "중단")
            },
            retryPolicy = RetryPolicy(maxAttempts = 5, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Aborted::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `CANCELLED 반환 시 재시도 없음`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("cancel-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.cancelled(ctx, "취소")
            },
            retryPolicy = RetryPolicy(maxAttempts = 5, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Cancelled::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `maxAttempts 1 - 재시도 없이 단일 실행`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("single-work") { ctx ->
                counter.incrementAndGet()
                WorkReport.failure(ctx, RuntimeException("실패"))
            },
            retryPolicy = RetryPolicy(maxAttempts = 1, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `지수 백오프 - 각 재시도 사이 delay 정책 적용`() = runTest {
        val counter = AtomicInteger(0)
        val flow = SuspendRetryFlow(
            work = SuspendWork("backoff-work") { ctx ->
                val cnt = counter.incrementAndGet()
                if (cnt < 3) WorkReport.failure(ctx, RuntimeException("시도 #$cnt 실패"))
                else WorkReport.success(ctx)
            },
            retryPolicy = RetryPolicy(
                maxAttempts = 5,
                delay = 0.milliseconds,
                backoffMultiplier = 2.0,
                maxDelay = 100.milliseconds,
            ),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }
}
