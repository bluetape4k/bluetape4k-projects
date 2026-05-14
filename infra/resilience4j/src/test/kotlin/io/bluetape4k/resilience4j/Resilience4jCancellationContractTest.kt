package io.bluetape4k.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.resilience4j.bulkhead.withBulkhead
import io.bluetape4k.resilience4j.circuitbreaker.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.circuitbreaker.decorateSuspendFunction1
import io.bluetape4k.resilience4j.circuitbreaker.withCircuitBreaker
import io.bluetape4k.resilience4j.ratelimiter.withRateLimiter
import io.bluetape4k.resilience4j.retry.withRetry
import io.bluetape4k.resilience4j.timelimiter.withTimeLimiter
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Resilience4jCancellationContractTest {

    @Test
    fun `suspend wrappers propagate cancellation unchanged`() = runTest {
        val cancellation = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            withCircuitBreaker(CircuitBreaker.ofDefaults("cancel-cb")) {
                throw cancellation
            }
        }.message shouldBeEqualTo cancellation.message

        assertFailsWith<CancellationException> {
            withRetry<String>(Retry.ofDefaults("cancel-retry")) {
                throw cancellation
            }
        }.message shouldBeEqualTo cancellation.message

        assertFailsWith<CancellationException> {
            withRateLimiter(nonWaitingRateLimiter("cancel-rl")) {
                throw cancellation
            }
        }.message shouldBeEqualTo cancellation.message

        assertFailsWith<CancellationException> {
            withBulkhead(Bulkhead.ofDefaults("cancel-bulkhead")) {
                throw cancellation
            }
        }.message shouldBeEqualTo cancellation.message

        assertFailsWith<CancellationException> {
            withTimeLimiter(TimeLimiter.ofDefaults("cancel-tl")) {
                throw cancellation
            }
        }.message shouldBeEqualTo cancellation.message
    }

    @Test
    fun `retry does not retry cancellation`() = runTest {
        val attempts = AtomicInteger(0)
        val retry = Retry.of("cancel-no-retry") {
            RetryConfig.custom<Any?>()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .build()
        }

        assertFailsWith<CancellationException> {
            withRetry<String>(retry) {
                attempts.incrementAndGet()
                throw CancellationException("cancelled")
            }
        }

        attempts.get() shouldBeEqualTo 1
    }

    @Test
    fun `parameterized decorators propagate cancellation`() = runTest {
        val cancellation = CancellationException("parameterized cancelled")
        val circuitBreaker = CircuitBreaker.ofDefaults("cancel-decorated-cb")

        assertFailsWith<CancellationException> {
            circuitBreaker.decorateSuspendFunction1 { _: String ->
                throw cancellation
            }("input")
        }.message shouldBeEqualTo cancellation.message

        assertFailsWith<CancellationException> {
            circuitBreaker.decorateSuspendBiFunction { _: String, _: String ->
                throw cancellation
            }("a", "b")
        }.message shouldBeEqualTo cancellation.message
    }

    private fun nonWaitingRateLimiter(name: String): RateLimiter =
        RateLimiter.of(name) {
            RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build()
        }
}
