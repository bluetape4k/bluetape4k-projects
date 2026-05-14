package io.bluetape4k.bucket4j.local

import com.github.benmanes.caffeine.cache.LoadingCache
import io.bluetape4k.bucket4j.validateBucketKeySize
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
 * Base provider for key-based local Bucket4j buckets.
 *
 * ## Contract
 * - [resolveBucket] rejects blank keys.
 * - The local cache key is `[keyPrefix] + key`.
 * - The prefixed key must be at most `MAX_BUCKET_KEY_BYTES` UTF-8 bytes.
 * - The same key resolves to the same cached local bucket until cache expiry.
 *
 * @property bucketConfiguration bucket configuration used for new buckets.
 * @property keyPrefix cache-key namespace prefix.
 */
abstract class AbstractLocalBucketProvider<T: LocalBucket>(
    protected val bucketConfiguration: BucketConfiguration,
    protected val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {
    companion object: KLogging() {
        const val DEFAULT_KEY_PREFIX = "bluetape4k.rate-limit.key."

        /** Maximum number of local bucket cache entries. */
        const val DEFAULT_CACHE_MAX_SIZE = 100_000L

        /** Access-based expiration for local bucket cache entries. */
        @JvmStatic
        val DEFAULT_CACHE_EXPIRE_AFTER_ACCESS: Duration = Duration.ofHours(6)
    }

    /**
     * Cache storing one local bucket per prefixed key.
     */
    protected open val cache: LoadingCache<String, T> by lazy {
        caffeine {
            executor(VirtualThreadExecutor)
            maximumSize(DEFAULT_CACHE_MAX_SIZE)
            expireAfterAccess(DEFAULT_CACHE_EXPIRE_AFTER_ACCESS)
        }.loadingCache {
            createBucket()
        }
    }

    /** Creates a new local bucket for cache misses. */
    protected abstract fun createBucket(): T

    /**
     * Returns the prefixed cache key for [key].
     *
     * ```kotlin
     * val provider = LocalBucketProvider(bucketConfiguration, keyPrefix = "app.rate.")
     * val cacheKey = provider.getBucketKey("user-42")
     * // cacheKey == "app.rate.user-42"
     * ```
     */
    protected open fun getBucketKey(key: String): String = "$keyPrefix$key"

    /**
     * Resolves the local bucket for [key].
     *
     * ```kotlin
     * val config = BucketConfiguration.builder()
     *     .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofSeconds(1))))
     *     .build()
     * val provider = LocalBucketProvider(config)
     * val bucket = provider.resolveBucket("user-42")
     * val result = bucket.tryConsumeAndReturnRemaining(1)
     * // result.remainingTokens == 9
     * ```
     *
     * @throws IllegalArgumentException when [key] is blank or the prefixed key
     * exceeds `MAX_BUCKET_KEY_BYTES`.
     */
    open fun resolveBucket(key: String): T {
        key.requireNotBlank("key")
        log.debug { "Loading local bucket. key=$key" }
        val bucketKey = validateBucketKeySize(getBucketKey(key))

        return cache
            .get(bucketKey)
            .apply {
                log.debug { "Resolved bucket for key[$bucketKey]: $this" }
            }
    }
}
