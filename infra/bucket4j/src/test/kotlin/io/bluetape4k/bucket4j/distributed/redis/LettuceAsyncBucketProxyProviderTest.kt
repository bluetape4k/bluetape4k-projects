package io.bluetape4k.bucket4j.distributed.redis

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.distributed.AbstractAsyncBucketProxyProviderTest
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class LettuceAsyncBucketProxyProviderTest: AbstractAsyncBucketProxyProviderTest() {

    companion object: KLoggingChannel()

    override val bucketProvider: AsyncBucketProxyProvider by lazy {

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

        AsyncBucketProxyProvider(proxyManager.asAsync(), defaultBucketConfiguration)
    }

    @Test
    fun `async prefix 는 redis key 에 한 번만 적용된다`() = runTest {
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
            val provider = AsyncBucketProxyProvider(proxyManager.asAsync(), defaultBucketConfiguration, prefix)

            provider.resolveBucket(key).tryConsume(1).await() shouldBeEqualTo true

            val storedKeys = sync.keys("$prefix*")
            storedKeys.any { it == "$prefix$key" } shouldBeEqualTo true
            storedKeys.forEach { it.shouldNotContain(prefix + prefix) }
        } finally {
            connection.close()
        }
    }
}
