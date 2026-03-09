package io.bluetape4k.bucket4j.local

import io.bluetape4k.logging.KLogging
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.SynchronizationStrategy

/**
 * Custom key 기반(예: `userId`, `tenantId`)으로 [LocalBucket]을 제공하는 provider 입니다.
 *
 * ## 동작/계약
 * - in-memory bucket을 key별 캐시에 보관합니다.
 * - 기본 구현은 lock-free synchronization 전략과 millisecond precision을 사용합니다.
 * - 같은 key를 재조회하면 동일한 로컬 버킷 상태를 공유합니다.
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
): AbstractLocalBucketProvider<LocalBucket>(bucketConfiguration, keyPrefix) {

    companion object: KLogging()

    /**
     * Local에서 사용하는 [LocalBucket]을 생성합니다.
     *
     * lock-free synchronization 전략을 사용해 단일 JVM 내 높은 처리량을 우선합니다.
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
