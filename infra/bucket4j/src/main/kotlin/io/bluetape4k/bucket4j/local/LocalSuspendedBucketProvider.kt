package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.coroutines.CoLocalBucket
import io.bluetape4k.bucket4j.coroutines.SuspendedLocalBucket
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter

/**
 * Custom key 기준(예: userId) 으로 Coroutines 환경에서 사용할 [CoLocalBucket]을 제공하는 Provider 입니다.
 *
 * ```
 * val bucketProvider = LocalCoBucketProvider(bucketConfiguration)
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
 * @see CoLocalBucket
 */
open class LocalSuspendedBucketProvider(
    bucketConfiguration: BucketConfiguration,
    keyPrefix: String = DEFAULT_KEY_PREFIX,
): AbstractLocalBucketProvider(bucketConfiguration, keyPrefix) {

    companion object: KLoggingChannel()

    /**
     * Coroutines용 [CoLocalBucket]을 생성합니다.
     *
     * @return [CoLocalBucket] 인스턴스
     */
    override fun createBucket(): SuspendedLocalBucket {
        log.debug { "Create CoLocalBucket ..." }

        return SuspendedLocalBucket(
            bucketConfiguration,
            MathType.INTEGER_64_BITS,
            TimeMeter.SYSTEM_MILLISECONDS
        )
    }
}
