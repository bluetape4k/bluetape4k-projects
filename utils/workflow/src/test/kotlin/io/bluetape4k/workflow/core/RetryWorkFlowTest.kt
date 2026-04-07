package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.AbstractWorkflowTest
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkReport
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class RetryWorkFlowTest: AbstractWorkflowTest() {

    @Test
    fun `첫 시도 성공 - Success 반환, 1회만 실행`() {
        val counter = AtomicInteger(0)
        val flow = RetryWorkFlow(
            work = Work("first-success") { ctx ->
                counter.incrementAndGet()
                WorkReport.success(ctx)
            },
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 1
    }

    @Test
    fun `N번 실패 후 성공 - maxAttempts 이내 성공`() {
        val counter = AtomicInteger(0)
        // 2번 실패 후 3번째 성공
        val flow = RetryWorkFlow(
            work = Work("fail-twice") { ctx ->
                val attempt = counter.incrementAndGet()
                if (attempt < 3) WorkReport.failure(ctx, RuntimeException("시도 #$attempt 실패"))
                else WorkReport.success(ctx)
            },
            retryPolicy = RetryPolicy(maxAttempts = 5, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report.isSuccess.shouldBeTrue()
        counter.get() shouldBeEqualTo 3
    }

    @Test
    fun `maxAttempts 소진 - Failure 반환, 총 maxAttempts번 실행`() {
        val maxAttempts = 3
        val counter = AtomicInteger(0)
        val flow = RetryWorkFlow(
            work = Work("always-fail") { ctx ->
                counter.incrementAndGet()
                WorkReport.failure(ctx, RuntimeException("항상 실패"))
            },
            retryPolicy = RetryPolicy(maxAttempts = maxAttempts, delay = 0.milliseconds),
        )

        val report = flow.execute(context)

        report shouldBeInstanceOf WorkReport.Failure::class
        counter.get() shouldBeEqualTo maxAttempts
    }

    @Test
    fun `ABORTED 반환 시 재시도 없음`() {
        val counter = AtomicInteger(0)
        val flow = RetryWorkFlow(
            work = Work("abort-work") { ctx ->
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
    fun `CANCELLED 반환 시 재시도 없음`() {
        val counter = AtomicInteger(0)
        val flow = RetryWorkFlow(
            work = Work("cancel-work") { ctx ->
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
    fun `maxRetries == maxAttempts - 1 검증`() {
        val policy = RetryPolicy(maxAttempts = 5, delay = 0.milliseconds)

        policy.maxRetries shouldBeEqualTo policy.maxAttempts - 1
        policy.maxRetries shouldBeEqualTo 4
    }
}
