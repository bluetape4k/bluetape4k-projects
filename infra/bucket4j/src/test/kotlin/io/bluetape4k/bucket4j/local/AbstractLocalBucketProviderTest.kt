package io.bluetape4k.bucket4j.local

import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

abstract class AbstractLocalBucketProviderTest {

    companion object: KLogging() {
        internal const val INITIAL_CAPACITY = 10L
    }

    abstract val bucketProvider: AbstractLocalBucketProvider

    protected fun randomKey(): String = "bucket-" + Base58.randomString(6)

    @Test
    fun `Custom key에 해당하는 Bucket을 제공한다`() {
        val key = randomKey()

        val bucket1 = bucketProvider.resolveBucket(key)
        val bucket2 = bucketProvider.resolveBucket(key)

        bucket1 shouldBeEqualTo bucket2
    }

    @Test
    fun `다른 key에 해당하는 Bucket을 제공한다`() {
        val key1 = randomKey()
        val key2 = randomKey()

        val bucket1 = bucketProvider.resolveBucket(key1)
        val bucket2 = bucketProvider.resolveBucket(key2)

        bucket2 shouldNotBeEqualTo bucket1
    }

    @Test
    fun `특정 키의 Bucket의 토큰을 사용환다`() {
        val key = randomKey()
        val bucket = bucketProvider.resolveBucket(key)

        val token = 5L
        val consumption = bucket.tryConsumeAndReturnRemaining(token)
        consumption.remainingTokens shouldBeEqualTo (INITIAL_CAPACITY - token)


        bucket.tryConsume(INITIAL_CAPACITY).shouldBeFalse()
        bucket.tryConsume(INITIAL_CAPACITY - token).shouldBeTrue()
    }
}
