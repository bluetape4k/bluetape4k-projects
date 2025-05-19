package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

abstract class AbstractAsyncBucketProxyProviderTest {

    companion object: KLoggingChannel() {
        internal const val INITIAL_TOKEN = 10L

        @JvmStatic
        val defaultBucketConfiguration by lazy {
            bucketConfiguration {
                addLimit {
                    it.capacity(INITIAL_TOKEN).refillIntervally(INITIAL_TOKEN, 10.seconds.toJavaDuration())
                }
            }
        }
    }

    protected abstract val bucketProvider: AsyncBucketProxyProvider

    protected fun randomKey(): String = "user-" + Base58.randomString(6)

    @Test
    fun `특정 Key에 해당하는 BucketProxy를 가져온다`() = runTest {
        val key = randomKey()

        val bucketProxy = bucketProvider.resolveBucket(key)
        bucketProxy.shouldNotBeNull()
        bucketProxy.availableTokens.await() shouldBeEqualTo INITIAL_TOKEN
    }

    @Test
    fun `같은 Key에 해당하는 BucketProxy이면 available token 수가 같다`() = runTest {
        val key = randomKey()

        val bucketProxy1 = bucketProvider.resolveBucket(key)
        val bucketProxy2 = bucketProvider.resolveBucket(key)

        bucketProxy1.availableTokens.await() shouldBeEqualTo bucketProxy2.availableTokens.await()
    }

    @Test
    fun `초기 토큰을 모두 사용한다`() = runTest {
        val key = randomKey()

        val bucketProxy1 = bucketProvider.resolveBucket(key)

        bucketProxy1.tryConsume(5).await().shouldBeTrue()
        bucketProxy1.tryConsume(INITIAL_TOKEN).await().shouldBeFalse()

        // 같은 키의 BucketProxy를 가져오면, 소비된 token 수가 적용된다.
        val bucketProxy2 = bucketProvider.resolveBucket(key)
        bucketProxy2.availableTokens.await() shouldBeEqualTo 5
    }
}
