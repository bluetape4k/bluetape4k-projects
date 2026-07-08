package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.lettuceBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceSuspendRateLimiterTest: AbstractSuspendRateLimiterTest() {

    companion object: KLoggingChannel()

    val bucketProvider: AsyncBucketProxyProvider by lazy {
        val redisClient = TestRedisServer.lettuceClient()
        val redissonProxyManager = lettuceBasedProxyManagerOf(redisClient) {
            ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        90.seconds.toJavaDuration()
                    )
                )
                .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
        }

        AsyncBucketProxyProvider(redissonProxyManager.asAsync(), defaultBucketConfiguration)
    }

    override val rateLimiter: SuspendRateLimiter<String> by lazy {
        DistributedSuspendRateLimiter(bucketProvider)
    }

    @Test
    fun `redis 장애 상황에서는 error 결과를 반환한다`() = runTest {
        val brokenProvider = mockk<AsyncBucketProxyProvider>()
        every { brokenProvider.resolveBucket(any()) } throws RuntimeException("simulated redis failure")

        val limiter = DistributedSuspendRateLimiter(brokenProvider)
        val result = limiter.consume(randomKey(), 1)
        result.status shouldBeEqualTo RateLimitStatus.ERROR
    }

    @Test
    fun `취소 예외는 전파해야 한다`() = runTest {
        val brokenProvider = mockk<AsyncBucketProxyProvider>()
        every { brokenProvider.resolveBucket(any()) } throws CancellationException("simulated cancellation")

        val limiter = DistributedSuspendRateLimiter(brokenProvider)

        assertFailsWith<CancellationException> {
            limiter.consume(randomKey(), 1)
        }
    }

    @Test
    fun `distributed await 중 취소되면 CancellationException 을 전파한다`() = runTest {
        val bucket = mockk<AsyncBucketProxy>()
        val pending = CompletableFuture<ConsumptionProbe>()
        every { bucket.tryConsumeAndReturnRemaining(any()) } returns pending

        val provider = mockk<AsyncBucketProxyProvider>()
        every { provider.resolveBucket(any()) } returns bucket

        val limiter = DistributedSuspendRateLimiter(provider)
        val task = async {
            limiter.consume(randomKey(), 1)
        }

        task.cancel(CancellationException("cancel distributed wait"))

        assertFailsWith<CancellationException> {
            task.await()
        }
    }

    @Test
    fun `distributed await 이 timeout cancellation 으로 실패하면 전파해야 한다`() = runTest {
        val bucket = mockk<AsyncBucketProxy>()
        val failed = CompletableFuture<ConsumptionProbe>()
        failed.completeExceptionally(callerTimeoutCancellation())
        every { bucket.tryConsumeAndReturnRemaining(any()) } returns failed

        val provider = mockk<AsyncBucketProxyProvider>()
        every { provider.resolveBucket(any()) } returns bucket

        val limiter = DistributedSuspendRateLimiter(provider)

        assertFailsWith<TimeoutCancellationException> {
            limiter.consume(randomKey(), 1)
        }
    }

    @Test
    fun `distributed await timeout 은 error 결과로 반환한다`() = runTest {
        val bucket = mockk<AsyncBucketProxy>()
        val pending = CompletableFuture<ConsumptionProbe>()
        every { bucket.tryConsumeAndReturnRemaining(any()) } returns pending

        val provider = mockk<AsyncBucketProxyProvider>()
        every { provider.resolveBucket(any()) } returns bucket

        val limiter = DistributedSuspendRateLimiter(provider, defaultTimeout = 10.milliseconds)
        val deferred = async {
            limiter.consume(randomKey(), 1)
        }

        advanceTimeBy(10.milliseconds)

        val result = deferred.await()
        result.status shouldBeEqualTo RateLimitStatus.ERROR
    }

    @Test
    fun `caller timeout 은 per-call timeout 없이도 전파해야 한다`() = runTest {
        val bucket = mockk<AsyncBucketProxy>()
        val pending = CompletableFuture<ConsumptionProbe>()
        every { bucket.tryConsumeAndReturnRemaining(any()) } returns pending

        val provider = mockk<AsyncBucketProxyProvider>()
        every { provider.resolveBucket(any()) } returns bucket

        val limiter = DistributedSuspendRateLimiter(provider)
        val task = async {
            withTimeout(10.milliseconds) {
                limiter.consume(randomKey(), 1, timeout = null)
            }
        }

        advanceTimeBy(10.milliseconds)
        runCurrent()

        assertFailsWith<TimeoutCancellationException> {
            task.await()
        }
    }

    @Test
    fun `caller timeout 은 default timeout 보다 먼저 발생하면 전파해야 한다`() = runTest {
        val bucket = mockk<AsyncBucketProxy>()
        val pending = CompletableFuture<ConsumptionProbe>()
        every { bucket.tryConsumeAndReturnRemaining(any()) } returns pending

        val provider = mockk<AsyncBucketProxyProvider>()
        every { provider.resolveBucket(any()) } returns bucket

        val limiter = DistributedSuspendRateLimiter(provider, defaultTimeout = 1.seconds)
        val task = async {
            withTimeout(10.milliseconds) {
                limiter.consume(randomKey(), 1)
            }
        }

        advanceTimeBy(10.milliseconds)
        runCurrent()

        assertFailsWith<TimeoutCancellationException> {
            task.await()
        }
    }

    private suspend fun callerTimeoutCancellation(): TimeoutCancellationException {
        try {
            withTimeout(1.milliseconds) {
                delay(2.milliseconds)
            }
        } catch (e: TimeoutCancellationException) {
            return e
        }
        error("withTimeout must throw TimeoutCancellationException")
    }
}
