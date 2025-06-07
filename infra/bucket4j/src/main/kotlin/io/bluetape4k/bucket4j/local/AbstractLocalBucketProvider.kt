package io.bluetape4k.bucket4j.local

import io.bluetape4k.bucket4j.internal.Slf4jBucketListener
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.caffeine.loadingCache
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.LockFreeBucket
import java.time.Duration


/**
 * Custom Key 기반 (예: userId) 의 Local Bucket을 제공합니다.
 *
 * @property bucketConfiguration [BucketConfiguration] 인스턴스
 * @property keyPrefix Bucket Key Prefix
 */
abstract class AbstractLocalBucketProvider(
    protected val bucketConfiguration: BucketConfiguration,
    protected val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {
    companion object: KLogging() {
        const val DEFAULT_KEY_PREFIX = "bluetape4k.rate-limit.key."
    }

    /**
     * Custom Key: [Bucket] 을 저장하는 캐시
     */
    protected open val cache by lazy {
        caffeine {
            executor(VirtualThreadExecutor)
            maximumSize(100000)
            expireAfterAccess(Duration.ofHours(6))
        }.loadingCache<String, LocalBucket> {
            createBucket()
        }
    }

    /**
     * Bucket을 생성합니다.
     *
     * @return [Bucket]
     */
    protected open fun createBucket(): LocalBucket {
        return LockFreeBucket(
            bucketConfiguration,
            MathType.INTEGER_64_BITS,
            TimeMeter.SYSTEM_MILLISECONDS,
            Slf4jBucketListener(log)
        )
    }

    protected open fun getBucketKey(key: String): String = "$keyPrefix$key"

    /**
     * [key]에 해당하는 [LocalBucket]을 제공합니다.
     *
     * @param key Custom Key
     * @return [LocalBucket] 인스턴스
     */
    fun resolveBucket(key: String): LocalBucket {
        log.debug { "Loading lcoal bucket. key=$key" }
        val bucketKey = getBucketKey(key)

        return cache.get(bucketKey)
            .apply {
                log.debug { "Resolved bucket for key[$bucketKey]: $this" }
            }
    }
}
