package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.MAX_BUCKET_KEY_BYTES
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.local.LocalBucket
import org.junit.jupiter.api.Test

abstract class AbstractLocalBucketProviderTest {

    companion object: KLogging() {
        internal const val INITIAL_CAPACITY = 10L
    }

    abstract val bucketProvider: AbstractLocalBucketProvider<out LocalBucket>

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

    @Test
    fun `빈 key 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            bucketProvider.resolveBucket(" ")
        }
    }

    @Test
    fun `serialized bucket key size cap 을 초과한 key 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            bucketProvider.resolveBucket("x".repeat(MAX_BUCKET_KEY_BYTES + 1))
        }
    }

    @Test
    fun `serialized bucket key size cap 은 prefix 포함 경계값으로 적용한다`() {
        val prefixBytes = AbstractLocalBucketProvider.DEFAULT_KEY_PREFIX.toByteArray().size
        val maxKey = "x".repeat(MAX_BUCKET_KEY_BYTES - prefixBytes)
        val oversizedKey = "x".repeat(MAX_BUCKET_KEY_BYTES - prefixBytes + 1)

        bucketProvider.resolveBucket(maxKey)

        assertFailsWith<IllegalArgumentException> {
            bucketProvider.resolveBucket(oversizedKey)
        }
    }
}
