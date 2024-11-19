package io.bluetape4k.bucket4j.distributed.redis

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AbstractBucketProxyProviderTest
import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceBucketProxyProviderTest: AbstractBucketProxyProviderTest() {

    companion object: KLogging()

    override val bucketProvider: BucketProxyProvider by lazy {

        val redisClient = TestRedisServer.lettuceClient()
        val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
            withClientSideConfig(
                ClientSideConfig.getDefault()
                    .withExpirationAfterWriteStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                            90.seconds.toJavaDuration()
                        )
                    )
                    .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
            )
        }

        BucketProxyProvider(proxyManager, defaultBucketConfiguration)
    }
}
