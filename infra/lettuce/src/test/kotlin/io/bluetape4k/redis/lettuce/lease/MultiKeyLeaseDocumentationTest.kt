package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.codec.Base58
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
import java.util.concurrent.atomic.AtomicInteger

internal class MultiKeyLeaseDocumentationTest {

    @Test
    fun `ambiguous Redis mutation remains recoverable`() = runSuspendIO {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName()
            val keys = listOf("ticket:{$tag}:ip", "ticket:{$tag}:user")
            val ownerToken = Base58.randomString(22)
            val commands = connection.sync()
            val lease = LettuceSuspendMultiKeyLease(connection)
            val retryable: (Throwable) -> Boolean = {
                it is IOException || it is RedisConnectionException || it is RedisCommandTimeoutException
            }
            val retry = Retry.of(
                "documentation-$tag",
                RetryConfig.custom<Any?>()
                    .maxAttempts(2)
                    .waitDuration(Duration.ofMillis(50))
                    .retryOnException(retryable)
                    .build(),
            )
            val circuitBreaker = CircuitBreaker.of(
                "documentation-$tag",
                CircuitBreakerConfig.custom()
                    .slidingWindowSize(20)
                    .minimumNumberOfCalls(10)
                    .failureRateThreshold(50.0F)
                    .recordException(retryable)
                    .ignoreException { it is CancellationException }
                    .build(),
            )
            val bulkhead = Bulkhead.of(
                "documentation-$tag",
                BulkheadConfig.custom()
                    .maxConcurrentCalls(32)
                    .maxWaitDuration(Duration.ofMillis(100))
                    .build(),
            )
            val attempts = AtomicInteger()
            try {
                commands.del(*keys.toTypedArray())
                val acquireResult = SuspendDecorators.ofSupplier {
                    val result = lease.acquire(keys, ownerToken, Duration.ofSeconds(10))
                    if (attempts.incrementAndGet() == 1) throw IOException("response lost after Redis success")
                    result
                }
                    .withRetry(retry)
                    .withCircuitBreaker(circuitBreaker)
                    .withBulkhead(bulkhead)
                    .invoke()

                acquireResult.shouldBeInstanceOf<MultiKeyAcquireResult.AlreadyOwned>()
                acquireAction(acquireResult) shouldBeEqualTo "recover"
                attempts.get() shouldBeEqualTo 2
                recoverAfterAmbiguousMutation(lease, keys, ownerToken)
                    .shouldBeInstanceOf<MultiKeyInspectResult.Owned>()
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    private suspend fun recoverAfterAmbiguousMutation(
        lease: LettuceSuspendMultiKeyLease,
        keys: List<String>,
        ownerToken: String,
    ): MultiKeyInspectResult = lease.inspect(keys, ownerToken)

    private fun acquireAction(result: MultiKeyAcquireResult): String = when (result) {
        MultiKeyAcquireResult.Acquired        -> "start"
        is MultiKeyAcquireResult.AlreadyOwned -> "recover"
        is MultiKeyAcquireResult.PartialOwnership -> "reconcile"
        is MultiKeyAcquireResult.Conflicted   -> "reject"
    }
}
