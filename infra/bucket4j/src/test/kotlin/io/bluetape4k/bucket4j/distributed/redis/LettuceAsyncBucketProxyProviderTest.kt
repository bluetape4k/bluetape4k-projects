package io.bluetape4k.bucket4j.distributed.redis

import io.bluetape4k.bucket4j.TestRedisServer
import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.bucket4j.distributed.AbstractAsyncBucketProxyProviderTest
import io.bluetape4k.bucket4j.distributed.AsyncBucketProxyProvider
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.TokensInheritanceStrategy
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.ExecutionStrategy
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.Duration
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

            provider.resolveBucket(key).tryConsume(1).await().shouldBeTrue()

            val storedKeys = sync.keys("$prefix*")
            storedKeys.any { it == "$prefix$key" }.shouldBeTrue()
            storedKeys.forEach { it.shouldNotContain(prefix + prefix) }
        } finally {
            connection.close()
        }
    }

    @Test
    fun `async proxy 는 identified bandwidth configuration replacement 를 지원한다`() = runTest {
        val redisClient = TestRedisServer.lettuceClient()
        val initial = bucketConfiguration {
            addLimit { it.capacity(10).refillGreedy(10, Duration.ofDays(1)).id("burst") }
        }
        val replacement = bucketConfiguration {
            addLimit { it.capacity(20).refillGreedy(20, Duration.ofDays(1)).id("burst") }
        }
        val proxyManager = lettuceBasedProxyManagerOf(redisClient) {
            ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(90.seconds.toJavaDuration())
                )
                .withExecutionStrategy(ExecutionStrategy.background(Executors.newVirtualThreadPerTaskExecutor()))
        }
        val provider = AsyncBucketProxyProvider(
            proxyManager.asAsync(),
            initial,
            "bluetape4k:rate-limit:replace:${Base58.randomString(6)}:",
        )
        val bucket = provider.resolveBucket("user-${Base58.randomString(6)}")

        bucket.tryConsume(5).await().shouldBeTrue()
        bucket.replaceConfiguration(replacement, TokensInheritanceStrategy.PROPORTIONALLY).await()

        bucket.availableTokens.await() shouldBeEqualTo 10
    }
}
