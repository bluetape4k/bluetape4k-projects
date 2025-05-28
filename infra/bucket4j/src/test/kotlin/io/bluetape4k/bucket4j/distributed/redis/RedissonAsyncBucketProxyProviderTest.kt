package io.bluetape4k.bucket4j.distributed.redis

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AbstractAsyncBucketProxyProviderTest
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedissonAsyncBucketProxyProviderTest: AbstractAsyncBucketProxyProviderTest() {

    companion object: KLoggingChannel()

    override val bucketProvider: AsyncBucketProxyProvider by lazy {

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
}
