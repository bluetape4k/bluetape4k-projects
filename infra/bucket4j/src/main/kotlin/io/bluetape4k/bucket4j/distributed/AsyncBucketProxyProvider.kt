package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.bucket4j.validateBucketKeySize
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.toUtf8Bytes
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.AsyncProxyManager

/**
 * Provider that resolves distributed Bucket4j [AsyncBucketProxy] instances by key.
 *
 * ## Contract
 * - [resolveBucket] rejects blank keys.
 * - The remote bucket key is `[keyPrefix] + key` encoded as UTF-8 bytes.
 * - The prefixed serialized key must be at most `MAX_BUCKET_KEY_BYTES`.
 * - Resolution only builds an async proxy and does not issue an extra
 *   remaining-token read.
 *
 * ```kotlin
 * class UserBasedAsyncBucketProvider(
 *    asyncProxyManager: AsyncProxyManager<ByteArray>,
 *    bucketConfiguration: BucketConfiguration,
 *    tokenPrefix: String
 * ): AsyncBucketProxyProvider(asyncProxyManager, bucketConfiguration, tokenPrefix) {
 *
 *     companion object: KLogging()
 *
 *     override fun getBucketKey(key: String): ByteArray {
 *          return "$tokenPrefix$key".toUtf8Bytes()
 *     }
 * }
 * ```
 *
 * @property asyncProxyManager Bucket4j async proxy manager.
 * @property bucketConfiguration bucket configuration used for new remote
 * buckets.
 * @property keyPrefix remote-key namespace prefix.
 */
open class AsyncBucketProxyProvider(
    protected val asyncProxyManager: AsyncProxyManager<ByteArray>,
    protected val bucketConfiguration: BucketConfiguration,
    protected val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {

    companion object: KLoggingChannel() {
        const val DEFAULT_KEY_PREFIX = BucketProxyProvider.DEFAULT_KEY_PREFIX
    }

    /**
     * Resolves the [AsyncBucketProxy] for [key].
     *
     * ## Contract
     * - [key] must not be blank.
     * - Calls with the same key point at the same remote bucket state.
     * - Future completion and remaining-token reads are caller-owned; this
     *   method only resolves the async proxy.
     */
    fun resolveBucket(key: String): AsyncBucketProxy {
        key.requireNotBlank("key")
        log.debug { "Resolving AsyncBucketProxy for key: $key" }
        // Keep prefix ownership in getBucketKey so overrides have one boundary.
        val bucketKey = validateBucketKeySize(getBucketKey(key))

        return asyncProxyManager.builder()
            .build(bucketKey) { completableFutureOf(bucketConfiguration) }
            .apply {
                log.debug { "Resolved async bucket for key[$key] with prefix[$keyPrefix]" }
            }
    }

    /**
     * Builds the serialized bucket key for the remote store.
     *
     * The default implementation prefixes [key] and encodes it as UTF-8 bytes.
     */
    protected open fun getBucketKey(key: String): ByteArray {
        return "$keyPrefix$key".toUtf8Bytes()
    }

}
