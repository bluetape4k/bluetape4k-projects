package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.resilience4j.SuspendDecorators
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/** Demonstrates caller-owned Retry, CircuitBreaker, and Bulkhead policies around the Redis primitive. */
internal class LettuceFencingLeaseResilience4jTest {

    @Test
    fun `backend failure alone is retried and the final result alone reaches the circuit breaker`() = runSuspendIO {
        val policy = policies("backend-final-result")
        val attempts = AtomicInteger()

        val result = execute(policy) {
            policy.bulkhead.metrics.availableConcurrentCalls.shouldBeZero()
            if (attempts.incrementAndGet() == 1) backendFailure else acquired
        }

        result shouldBeEqualTo acquired
        attempts.get() shouldBeEqualTo 2
        policy.retry.metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
        policy.circuitBreaker.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
        policy.circuitBreaker.metrics.numberOfFailedCalls.shouldBeZero()
        policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
    }

    @Test
    fun `terminal backend failure is recorded once after retry exhaustion`() = runSuspendIO {
        val policy = policies("terminal-backend")
        val attempts = AtomicInteger()

        execute(policy) {
            policy.bulkhead.metrics.availableConcurrentCalls.shouldBeZero()
            attempts.incrementAndGet()
            backendFailure
        } shouldBeEqualTo backendFailure

        attempts.get() shouldBeEqualTo 2
        policy.retry.metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 1
        policy.circuitBreaker.metrics.numberOfFailedCalls shouldBeEqualTo 1
        policy.circuitBreaker.metrics.numberOfSuccessfulCalls.shouldBeZero()
        policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
    }

    @Test
    fun `ambiguous acquire retries with the same owner and recovers the Redis token`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName().substringAfter(':')
            val config = LettuceFencingLeaseConfig("resilience", tag, 31)
            val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
            val lease = LettuceSuspendFencingLease(connection, config)
            val ownerId = FencingOwnerId.from("owner-$tag")
            val attempts = AtomicInteger()
            try {
                lease.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

                val result = execute(policies("ambiguous-$tag")) {
                    val actual = lease.acquire(ownerId, LEASE_TIME)
                    if (attempts.incrementAndGet() == 1) backendFailure else actual
                }.shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()

                attempts.get() shouldBeEqualTo 2
                connection.sync().get(keys.counter) shouldBeEqualTo result.token.sequence.toString()
            } finally {
                connection.sync().del(keys.lease, keys.counter)
            }
        }
    }

    @Test
    fun `domain results are logical successes and never retry`() = runSuspendIO {
        listOf(
            FencingAcquireResult.Contended(1),
            FencingAcquireResult.CounterUnavailable,
            FencingAcquireResult.SequenceExhausted,
            FencingAcquireResult.IntegrityFailure(
                FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.INVALID_COUNTER),
            ),
        ).forEachIndexed { index, result ->
            assertLogicalSuccessOnce(policies("logical-$index"), result)
        }

        assertRenewLogicalSuccessOnce(FencingRenewResult.OwnershipMismatch)
    }

    @Test
    fun `validation cancellation and protocol exceptions escape every policy`() = runSuspendIO {
        listOf(
            IllegalArgumentException("invalid input") to 1L,
            CancellationException("caller cancelled") to 0L,
            FencingLeaseProtocolException() to 1L,
        ).forEachIndexed { index, (expected, failedWithoutRetry) ->
            val policy = policies("exception-$index")
            val attempts = AtomicInteger()

            val actual = assertFailsWith<Throwable> {
                execute(policy) {
                    attempts.incrementAndGet()
                    throw expected
                }
            }

            actual shouldBeSameInstanceAs expected
            attempts.get() shouldBeEqualTo 1
            policy.retry.metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo failedWithoutRetry
            policy.retry.metrics.numberOfFailedCallsWithRetryAttempt.shouldBeZero()
            with(policy.circuitBreaker.metrics) {
                numberOfSuccessfulCalls.shouldBeZero()
                numberOfFailedCalls.shouldBeZero()
                numberOfBufferedCalls.shouldBeZero()
            }
            policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
        }
    }

    @Test
    fun `open circuit breaker and saturated bulkhead reject before inner policies`() = runSuspendIO {
        val openPolicy = policies("open-circuit")
        openPolicy.circuitBreaker.transitionToOpenState()
        val openAttempts = AtomicInteger()

        assertFailsWith<CallNotPermittedException> {
            execute(openPolicy) {
                openAttempts.incrementAndGet()
                acquired
            }
        }
        openAttempts.get().shouldBeZero()
        assertRetryUntouched(openPolicy.retry)
        with(openPolicy.circuitBreaker.metrics) {
            numberOfSuccessfulCalls.shouldBeZero()
            numberOfFailedCalls.shouldBeZero()
        }

        val fullPolicy = policies("full-bulkhead")
        fullPolicy.bulkhead.tryAcquirePermission().shouldBeTrue()
        val fullAttempts = AtomicInteger()
        try {
            assertFailsWith<BulkheadFullException> {
                execute(fullPolicy) {
                    fullAttempts.incrementAndGet()
                    acquired
                }
            }
            fullAttempts.get().shouldBeZero()
            assertRetryUntouched(fullPolicy.retry)
            with(fullPolicy.circuitBreaker.metrics) {
                numberOfSuccessfulCalls.shouldBeZero()
                numberOfFailedCalls.shouldBeZero()
                numberOfBufferedCalls.shouldBeZero()
            }
        } finally {
            fullPolicy.bulkhead.releasePermission()
        }
    }

    private suspend fun assertLogicalSuccessOnce(
        policy: Policies,
        result: FencingAcquireResult,
    ) {
        val attempts = AtomicInteger()

        execute(policy) {
            attempts.incrementAndGet()
            result
        } shouldBeEqualTo result

        attempts.get() shouldBeEqualTo 1
        policy.retry.metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
        policy.retry.metrics.numberOfSuccessfulCallsWithRetryAttempt.shouldBeZero()
        policy.circuitBreaker.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
        policy.circuitBreaker.metrics.numberOfFailedCalls.shouldBeZero()
    }

    private suspend fun assertRenewLogicalSuccessOnce(result: FencingRenewResult) {
        val retry = Retry.of(
            "renew-logical",
            RetryConfig.custom<FencingRenewResult>()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .retryOnResult { it is FencingRenewResult.BackendFailure }
                .retryOnException { false }
                .build(),
        )
        val attempts = AtomicInteger()

        SuspendDecorators.ofSupplier {
            attempts.incrementAndGet()
            result
        }.withRetry(retry).invoke() shouldBeEqualTo result

        attempts.get() shouldBeEqualTo 1
        retry.metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
    }

    private suspend fun execute(
        policy: Policies,
        supplier: suspend () -> FencingAcquireResult,
    ): FencingAcquireResult = SuspendDecorators.ofSupplier(supplier)
        .withRetry(policy.retry)
        .withCircuitBreaker(policy.circuitBreaker)
        .withBulkhead(policy.bulkhead)
        .invoke()

    private fun policies(name: String): Policies = Policies(
        retry = Retry.of(
            name,
            RetryConfig.custom<FencingAcquireResult>()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .retryOnResult(::isBackendFailure)
                .retryOnException { false }
                .build(),
        ),
        circuitBreaker = CircuitBreaker.of(
            name,
            CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0F)
                .recordResult { result -> result is FencingAcquireResult.BackendFailure }
                .ignoreException { true }
                .build(),
        ),
        bulkhead = Bulkhead.of(
            name,
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build(),
        ),
    )

    private fun assertRetryUntouched(retry: Retry) {
        with(retry.metrics) {
            numberOfSuccessfulCallsWithoutRetryAttempt.shouldBeZero()
            numberOfSuccessfulCallsWithRetryAttempt.shouldBeZero()
            numberOfFailedCallsWithoutRetryAttempt.shouldBeZero()
            numberOfFailedCallsWithRetryAttempt.shouldBeZero()
        }
    }

    private fun isBackendFailure(result: FencingAcquireResult): Boolean =
        result is FencingAcquireResult.BackendFailure

    private data class Policies(
        val retry: Retry,
        val circuitBreaker: CircuitBreaker,
        val bulkhead: Bulkhead,
    )

    private companion object {
        val LEASE_TIME: Duration = Duration.ofSeconds(30)
        val acquired = FencingAcquireResult.Acquired(FencingToken(31, 1))
        val backendFailure = FencingAcquireResult.BackendFailure(
            FencingLeaseBackendFailure(FencingBackendFailureKind.CONNECTION),
        )
    }
}
