package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.redissonBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedissonSuspendRateLimiterTest: AbstractSuspendRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider: AsyncBucketProxyProvider by lazy {

        val redisson = TestRedisServer.redissonClient()

        val redissonProxyManager = redissonBasedProxyManagerOf(redisson) {
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
        every { brokenProvider.resolveBucket(any()) } throws RuntimeException("simulated redisson failure")

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
}
