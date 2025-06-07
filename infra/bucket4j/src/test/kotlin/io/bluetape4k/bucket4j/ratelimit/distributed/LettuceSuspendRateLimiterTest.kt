package io.bluetape4k.bucket4j.ratelimit.distributed

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.bucket4j.distributed.redis.lettuceBasedProxyManagerOf
import io.bluetape4k.bucket4j.ratelimit.AbstractSuspendRateLimiterTest
import io.bluetape4k.bucket4j.ratelimit.SuspendRateLimiter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import java.util.concurrent.Executors
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

}
