package io.bluetape4k.bucket4j.local

import io.bluetape4k.logging.KLogging
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.SynchronizationStrategy

/**
 * Custom Key 기반 (예: userId) 으로 [LocalBucket]을 제공합니다.
 *
 * ```
 * val bucketProvider = LocalBucketProvider(bucketConfiguration)
 * val key = randomKey()
 * val bucket = bucketProvider.resolveBucket(key)
 * val token = 5L
 * val consumption = bucket.tryConsumeAndReturnRemaining(token)
 *
 * consumption.remainingTokens shouldBeEqualTo (INITIAL_CAPACITY - token)
 * bucket.tryConsume(INITIAL_CAPACITY).shouldBeFalse()
 * bucket.tryConsume(INITIAL_CAPACITY - token).shouldBeTrue()
 * ```
 *
 * @property bucketConfiguration [BucketConfiguration] 인스턴스
 * @property keyPrefix Bucket Key Prefix
 */
open class LocalBucketProvider(
    bucketConfiguration: BucketConfiguration,
    keyPrefix: String = DEFAULT_KEY_PREFIX,
): AbstractLocalBucketProvider(bucketConfiguration, keyPrefix) {

    companion object: KLogging()

    /**
     * Local에서 사용하는 [LocalBucket] 을 생성합니다.
     *
     * @return [LocalBucket]을 반환합니다.
     */
    override fun createBucket(): LocalBucket {
        val builder = Bucket.builder()
            .withSynchronizationStrategy(SynchronizationStrategy.LOCK_FREE)
            // .withMath(MathType.INTEGER_64_BITS)
            .withMillisecondPrecision()

        bucketConfiguration.bandwidths.forEach {
            builder.addLimit(it)
        }

        return builder.build()
    }
}
