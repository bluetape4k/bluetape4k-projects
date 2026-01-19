package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.coroutines.SuspendLocalBucket
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.local.LocalBucket

/**
 * Custom key 기준(예: userId) 으로 Coroutines 환경에서 사용할 [SuspendLocalBucket]을 제공하는 Provider 입니다.
 *
 * ```
 * val bucketProvider = LocalSuspendBucketProvider(bucketConfiguration)
 * val key = randomKey()
 * val bucket = bucketProvider.resolveBucket(key)
 *
 * val token = 5L
 * val consumption = bucket.tryConsumeAndReturnRemaining(token)
 *
 * consumption.remainingTokens shouldBeEqualTo (INITIAL_CAPACITY - token)
 * bucket.tryConsume(INITIAL_CAPACITY).shouldBeFalse()
 * bucket.tryConsume(INITIAL_CAPACITY - token).shouldBeTrue()
 * ```
 *
 * @param bucketConfiguration [BucketConfiguration] 인스턴스
 * @param keyPrefix Bucket Key Prefix
 *
 * @see SuspendLocalBucket
 */
open class LocalSuspendBucketProvider(
    bucketConfiguration: BucketConfiguration,
    keyPrefix: String = DEFAULT_KEY_PREFIX,
): AbstractLocalBucketProvider(bucketConfiguration, keyPrefix) {

    companion object: KLoggingChannel()

    /**
     * Coroutines용 [SuspendLocalBucket]을 생성합니다.
     *
     * @return [SuspendLocalBucket] 인스턴스
     */
    override fun createBucket(): SuspendLocalBucket {
        log.debug { "Create SuspendLocalBucket ..." }

        return SuspendLocalBucket(
            bucketConfiguration,
            MathType.INTEGER_64_BITS,
            TimeMeter.SYSTEM_MILLISECONDS
        )
    }

    /**
     * [key]에 해당하는 [SuspendLocalBucket]을 제공합니다.
     *
     * @param key Custom Key
     * @return [LocalBucket] 인스턴스
     */
    override fun resolveBucket(key: String): SuspendLocalBucket {
        log.debug { "Loading lcoal bucket. key=$key" }
        val bucketKey = getBucketKey(key)

        val bucket = cache.get(bucketKey) as SuspendLocalBucket
        log.debug { "Resolved bucket for key[$bucketKey]: $bucket" }
        return bucket
    }
}
