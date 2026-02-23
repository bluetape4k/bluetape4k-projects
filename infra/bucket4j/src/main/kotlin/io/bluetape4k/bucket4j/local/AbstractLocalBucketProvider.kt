package io.bluetape4k.bucket4j.local

import com.github.benmanes.caffeine.cache.LoadingCache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.caffeine.loadingCache
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.local.LocalBucket
import java.time.Duration


/**
 * Custom Key 기반 (예: userId) 의 Local Bucket을 제공합니다.
 *
 * [resolveBucket]은 빈 key를 허용하지 않으며, key prefix를 적용한 캐시 키로 버킷을 조회합니다.
 *
 * @property bucketConfiguration [BucketConfiguration] 인스턴스
 * @property keyPrefix Bucket Key Prefix
 */
abstract class AbstractLocalBucketProvider<T: LocalBucket>(
    protected val bucketConfiguration: BucketConfiguration,
    protected val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {
    companion object: KLogging() {
        const val DEFAULT_KEY_PREFIX = "bluetape4k.rate-limit.key."
    }

    /**
     * Custom Key: [Bucket] 을 저장하는 캐시
     */
    protected open val cache: LoadingCache<String, T> by lazy {
        caffeine {
            executor(VirtualThreadExecutor)
            maximumSize(100000)
            expireAfterAccess(Duration.ofHours(6))
        }.loadingCache {
            createBucket()
        }
    }

    /**
     * Bucket을 생성합니다.
     *
     * @return [Bucket]
     */
    protected abstract fun createBucket(): T

    protected open fun getBucketKey(key: String): String = "$keyPrefix$key"

    /**
     * [key]에 해당하는 [LocalBucket]을 제공합니다.
     *
     * @param key Custom Key
     * @return [LocalBucket] 인스턴스
     * @throws IllegalArgumentException key가 blank인 경우
     */
    open fun resolveBucket(key: String): T {
        key.requireNotBlank("key")
        log.debug { "Loading local bucket. key=$key" }
        val bucketKey = getBucketKey(key)

        return cache.get(bucketKey)
            .apply {
                log.debug { "Resolved bucket for key[$bucketKey]: $this" }
            }
    }
}
