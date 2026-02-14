package io.bluetape4k.bucket4j.distributed.redis

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AbstractBucketProxyProviderTest
import io.bluetape4k.bucket4j.distributed.BucketProxyProvider
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceBucketProxyProviderTest: AbstractBucketProxyProviderTest() {

    companion object: KLogging()

    override val bucketProvider: BucketProxyProvider by lazy {

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

    @Test
    fun `prefix 는 redis key 에 한 번만 적용된다`() {
        val redisClient = TestRedisServer.lettuceClient()
        val connection = redisClient.connect()
        val sync = connection.sync()
        val prefix = "bluetape4k:rate-limit:test:${Base58.randomString(6)}:"
        val key = "user-${Base58.randomString(6)}"

        try {
            val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
                ClientSideConfig.getDefault()
                    .withExpirationAfterWriteStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(90.seconds.toJavaDuration())
                    )
                    .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
            }
            val provider = BucketProxyProvider(proxyManager, defaultBucketConfiguration, prefix)

            provider.resolveBucket(key).tryConsume(1) shouldBeEqualTo true

            val storedKeys = sync.keys("$prefix*")
            storedKeys.any { it == "$prefix$key" } shouldBeEqualTo true
            storedKeys.forEach { it.shouldNotContain(prefix + prefix) }
        } finally {
            connection.close()
        }
    }
}
