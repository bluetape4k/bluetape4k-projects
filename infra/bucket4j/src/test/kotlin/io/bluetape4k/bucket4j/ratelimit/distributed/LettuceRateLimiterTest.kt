package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.lettuceBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.RateLimitStatus
import io.bluetape4k.bucket4j.ratelimit.RateLimiter
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceRateLimiterTest: AbstractRateLimiterTest() {

    companion object: KLogging()

    val bucketProvider: BucketProxyProvider by lazy {
        val redisClient = TestRedisServer.lettuceClient()
        val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
            ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        90.seconds.toJavaDuration()
                    )
                )
                .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
        }

        BucketProxyProvider(proxyManager, defaultBucketConfiguration)
    }

    override val rateLimiter: RateLimiter<String> by lazy {
        DistributedRateLimiter(bucketProvider)
    }

    @Test
    fun `redis 장애 상황에서는 error 결과를 반환한다`() {
        val brokenProvider = mockk<BucketProxyProvider>()
        every { brokenProvider.resolveBucket(any()) } throws RuntimeException("simulated redis failure")

        val limiter = DistributedRateLimiter(brokenProvider)
        val result = limiter.consume(randomKey(), 1)
        result.status shouldBeEqualTo RateLimitStatus.ERROR
    }

    @Test
    fun `CancellationException 은 ERROR 로 변환되지 않고 그대로 전파되어야 한다`() {
        // CancellationException 은 Exception 하위 타입이므로 명시적으로 재전파하지 않으면 ERROR 결과로 변환될 수 있다.
        val brokenProvider = mockk<BucketProxyProvider>()
        every { brokenProvider.resolveBucket(any()) } throws CancellationException("simulated cancellation")

        val limiter = DistributedRateLimiter(brokenProvider)

        assertFailsWith<CancellationException> {
            limiter.consume(randomKey(), 1)
        }
    }
}
