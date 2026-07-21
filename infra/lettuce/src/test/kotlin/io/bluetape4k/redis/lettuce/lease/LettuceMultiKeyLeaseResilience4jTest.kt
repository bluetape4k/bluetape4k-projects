package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.resilience4j.SuspendDecorators
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

internal class LettuceMultiKeyLeaseResilience4jTest {

    @Test
    fun `ambiguous acquire response retries with the same token and recovers replay`() = runSuspendIO {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = UUID.randomUUID().toString()
            val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
            val token = "owner-$tag"
            val commands = connection.sync()
            val lease = LettuceSuspendMultiKeyLease(connection)
            val policy = policies("ambiguous-$tag")
            val attempts = AtomicInteger()
            try {
                commands.del(*keys.toTypedArray())

                val result = execute(policy) {
                    val attempt = attempts.incrementAndGet()
                    val acquireResult = lease.acquire(keys, token, TEN_SECONDS)
                    if (attempt == 1) throw IOException("response lost after Redis success")
                    acquireResult
                }

                result.shouldBeInstanceOf<MultiKeyAcquireResult.AlreadyOwned>()
                attempts.get() shouldBeEqualTo 2
                policy.retry.metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
                policy.circuitBreaker.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
                policy.circuitBreaker.metrics.numberOfFailedCalls shouldBeEqualTo 0
                policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    @Test
    fun `domain results remain logical successes and are never retried`() = runSuspendIO {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = UUID.randomUUID().toString()
            val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
            val token = "owner-$tag"
            val commands = connection.sync()
            val lease = LettuceSuspendMultiKeyLease(connection)
            try {
                commands.del(*keys.toTypedArray())
                commands.psetex(keys[0], 5_000, "other-$tag")
                assertLogicalSuccessOnce(
                    policies("conflicted-$tag"),
                    MultiKeyAcquireResult.Conflicted(MultiKeyLeaseCounts(2, 0, 1, 1)),
                ) { lease.acquire(keys, token, TEN_SECONDS) }

                commands.del(*keys.toTypedArray())
                commands.psetex(keys[0], 5_000, token)
                assertLogicalSuccessOnce(
                    policies("partial-$tag"),
                    MultiKeyAcquireResult.PartialOwnership(MultiKeyLeaseCounts(2, 1, 1, 0)),
                ) { lease.acquire(keys, token, TEN_SECONDS) }
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    @Test
    fun `validation integrity and cancellation failures are never retried or leaked`() = runSuspendIO {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = UUID.randomUUID().toString()
            val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
            val token = "owner-$tag"
            val commands = connection.sync()
            val lease = LettuceSuspendMultiKeyLease(connection)
            try {
                assertRejectedOnce<IllegalArgumentException>(policies("validation-$tag")) {
                    lease.acquire(emptyList(), token, TEN_SECONDS)
                }

                commands.set(keys[0], token)
                commands.psetex(keys[1], 5_000, token)
                assertRejectedOnce<MultiKeyLeaseIntegrityException>(policies("integrity-$tag")) {
                    lease.acquire(keys, token, TEN_SECONDS)
                }

                assertCancellationExcluded(policies("cancellation-$tag")) {
                    throw CancellationException("caller cancelled")
                }
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    @Test
    fun `retry predicate accepts only approved ambiguous transport failures`() {
        listOf(
            IOException("response lost"),
            RedisConnectionException("connection lost"),
            RedisCommandTimeoutException("command timed out"),
        ).forEach { failure ->
            isAmbiguousTransportFailure(failure) shouldBeEqualTo true
        }
        listOf(
            IllegalArgumentException("invalid input"),
            MultiKeyLeaseIntegrityException(MultiKeyLeaseOperation.ACQUIRE, 2, 1),
            CancellationException("caller cancelled"),
        ).forEach { failure ->
            isAmbiguousTransportFailure(failure) shouldBeEqualTo false
        }
    }

    private suspend fun <T> execute(
        policy: Policies,
        supplier: suspend () -> T,
    ): T = SuspendDecorators.ofSupplier(supplier)
        .withRetry(policy.retry)
        .withCircuitBreaker(policy.circuitBreaker)
        .withBulkhead(policy.bulkhead)
        .invoke()

    private suspend fun <T> assertLogicalSuccessOnce(
        policy: Policies,
        expected: T,
        supplier: suspend () -> T,
    ) {
        val attempts = AtomicInteger()

        execute(policy) {
            attempts.incrementAndGet()
            supplier()
        } shouldBeEqualTo expected

        attempts.get() shouldBeEqualTo 1
        policy.retry.metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
        policy.retry.metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        policy.circuitBreaker.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
        assertNoFailureOrPermitLeak(policy)
    }

    private suspend inline fun <reified E: Throwable> assertRejectedOnce(
        policy: Policies,
        crossinline supplier: suspend () -> MultiKeyAcquireResult,
    ) {
        val attempts = AtomicInteger()

        assertFailsWith<E> {
            execute(policy) {
                attempts.incrementAndGet()
                supplier()
            }
        }

        attempts.get() shouldBeEqualTo 1
        policy.retry.metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        assertNoFailureOrPermitLeak(policy)
    }

    private suspend fun assertCancellationExcluded(
        policy: Policies,
        supplier: suspend () -> MultiKeyAcquireResult,
    ) {
        val attempts = AtomicInteger()

        assertFailsWith<CancellationException> {
            execute(policy) {
                attempts.incrementAndGet()
                supplier()
            }
        }

        attempts.get() shouldBeEqualTo 1
        with(policy.retry.metrics) {
            numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
            numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
            numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0
        }
        with(policy.circuitBreaker.metrics) {
            numberOfSuccessfulCalls shouldBeEqualTo 0
            numberOfFailedCalls shouldBeEqualTo 0
            numberOfBufferedCalls shouldBeEqualTo 0
        }
        policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
    }

    private fun assertNoFailureOrPermitLeak(policy: Policies) {
        policy.circuitBreaker.metrics.numberOfFailedCalls shouldBeEqualTo 0
        policy.bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo 1
    }

    private fun policies(name: String): Policies {
        val retry = Retry.of(
            name,
            RetryConfig.custom<Any?>()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .retryOnException(::isAmbiguousTransportFailure)
                .build(),
        )
        val circuitBreaker = CircuitBreaker.of(
            name,
            CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0F)
                .recordException(::isAmbiguousTransportFailure)
                .ignoreException { it is CancellationException }
                .build(),
        )
        val bulkhead = Bulkhead.of(
            name,
            BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ZERO)
                .build(),
        )
        return Policies(retry, circuitBreaker, bulkhead)
    }

    private fun isAmbiguousTransportFailure(error: Throwable): Boolean =
        error is IOException || error is RedisConnectionException || error is RedisCommandTimeoutException

    private data class Policies(
        val retry: Retry,
        val circuitBreaker: CircuitBreaker,
        val bulkhead: Bulkhead,
    )

    private companion object {
        val TEN_SECONDS: Duration = Duration.ofSeconds(10)
    }
}
