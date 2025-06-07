package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.lettuceBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendedRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.SuspendedRateLimiter
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceSuspendedRateLimiterTest: AbstractSuspendedRateLimiterTest() {

    companion object: KLogging()

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

    override val rateLimiter: SuspendedRateLimiter<String> by lazy {
        DistributedSuspendedRateLimiter(bucketProvider)
    }

}
