package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.redissonBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedissonRateLimiterTest: AbstractRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider: BucketProxyProvider by lazy {

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

        BucketProxyProvider(redissonProxyManager, defaultBucketConfiguration)
    }

    override val rateLimiter: RateLimiter<String> by lazy {
        DistributedRateLimiter(bucketProvider)
    }

    @Test
    fun `redis 장애 상황에서는 error 결과를 반환한다`() {
        val brokenProvider = mockk<BucketProxyProvider>()
        every { brokenProvider.resolveBucket(any()) } throws RuntimeException("simulated redisson failure")

        val limiter = DistributedRateLimiter(brokenProvider)
        val result = limiter.consume(randomKey(), 1)
        result.status shouldBeEqualTo RateLimitStatus.ERROR
    }
}
