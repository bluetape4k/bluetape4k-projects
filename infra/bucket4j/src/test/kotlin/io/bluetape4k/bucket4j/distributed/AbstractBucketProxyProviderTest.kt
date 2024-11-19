package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.bucket4j.bucketConfiguration
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

abstract class AbstractBucketProxyProviderTest {

    companion object: KLogging() {
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

    protected abstract val bucketProvider: BucketProxyProvider

    protected fun randomKey(): String = "user-" + Base58.randomString(6)

    @Test
    fun `특정 Key에 해당하는 BucketProxy를 가져온다`() {
        val key = randomKey()

        val bucketProxy = bucketProvider.resolveBucket(key)
        bucketProxy.availableTokens shouldBeEqualTo INITIAL_TOKEN

    }

    @Test
    fun `같은 Key에 해당하는 BucketProxy이면 available token 수가 같다`() {
        val key = randomKey()

        val bucketProxy1 = bucketProvider.resolveBucket(key)
        val bucketProxy2 = bucketProvider.resolveBucket(key)

        bucketProxy1.availableTokens shouldBeEqualTo bucketProxy2.availableTokens
    }

    @Test
    fun `초기 토큰을 모두 사용한다`() {
        val key = randomKey()

        val bucketProxy1 = bucketProvider.resolveBucket(key)

        bucketProxy1.tryConsume(5).shouldBeTrue()
        bucketProxy1.tryConsume(INITIAL_TOKEN).shouldBeFalse()

        // 같은 키의 BucketProxy를 가져오면, 소비된 token 수가 적용된다.
        val bucketProxy2 = bucketProvider.resolveBucket(key)
        bucketProxy2.availableTokens shouldBeEqualTo 5
    }
}
