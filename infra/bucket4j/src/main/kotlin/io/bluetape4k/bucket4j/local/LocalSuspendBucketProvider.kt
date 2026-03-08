package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.coroutines.SuspendLocalBucket
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter

/**
 * Coroutines 환경에서 사용할 [SuspendLocalBucket]을 key별로 제공하는 provider 입니다.
 *
 * ## 동작/계약
 * - 같은 key를 재조회하면 동일한 로컬 버킷 상태를 공유합니다.
 * - 기본 구현은 millisecond time meter와 64-bit math를 사용합니다.
 * - 소비 대기 자체는 [SuspendLocalBucket]이 `delay`로 처리하므로 호출 스레드를 블로킹하지 않습니다.
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
): AbstractLocalBucketProvider<SuspendLocalBucket>(bucketConfiguration, keyPrefix) {

    companion object: KLoggingChannel()

    /**
     * Coroutines용 [SuspendLocalBucket]을 생성합니다.
     *
     * 기본 수학/시간 설정은 `INTEGER_64_BITS`, `SYSTEM_MILLISECONDS` 입니다.
     */
    override fun createBucket(): SuspendLocalBucket {
        log.debug { "Create SuspendLocalBucket ..." }

        return SuspendLocalBucket(
            bucketConfiguration,
            MathType.INTEGER_64_BITS,
            TimeMeter.SYSTEM_MILLISECONDS
        )
    }
}
