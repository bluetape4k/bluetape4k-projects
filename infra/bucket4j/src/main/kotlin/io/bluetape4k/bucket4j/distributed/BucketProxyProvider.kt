package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.bucket4j.validateBucketKeySize
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.toUtf8Bytes
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.distributed.proxy.ProxyManager

/**
 * Provider that resolves distributed Bucket4j [BucketProxy] instances by key.
 *
 * ## Contract
 * - [resolveBucket] rejects blank keys.
 * - The remote bucket key is `[keyPrefix] + key` encoded as UTF-8 bytes.
 * - The prefixed serialized key must be at most `MAX_BUCKET_KEY_BYTES`.
 * - Resolution only builds a proxy and does not issue an extra remaining-token
 *   read.
 *
 * ```kotlin
 * class UserBasedBucketProvider(
 *    proxyManager: ProxyManager<ByteArray>,
 *    bucketConfiguration: BucketConfiguration,
 *    keyPrefix: String
 * ): BucketProxyProvider(proxyManager, bucketConfiguration, keyPrefix) {
 *
 *     companion object: KLogging()
 *
 *     override fun getBucketKey(key: String): ByteArray {
 *          return "$keyPrefix$key".toUtf8Bytes()
 *     }
 * }
 * ```
 *
 * @property proxyManager Bucket4j proxy manager.
 * @property bucketConfiguration bucket configuration used for new remote
 * buckets.
 * @property keyPrefix remote-key namespace prefix.
 */
open class BucketProxyProvider(
    protected val proxyManager: ProxyManager<ByteArray>,
    protected val bucketConfiguration: BucketConfiguration,
    protected val keyPrefix: String = DEFAULT_KEY_PREFIX,
) {

    companion object: KLogging() {
        const val DEFAULT_KEY_PREFIX = "bluetape4k:rate-limit:key:"
    }

    /**
     * Resolves the [BucketProxy] for [key].
     *
     * ## Contract
     * - [key] must not be blank.
     * - Calls with the same key point at the same remote bucket state.
     * - Remaining-token reads are caller-owned; this method only resolves the
     *   proxy.
     */
    fun resolveBucket(key: String): BucketProxy {
        key.requireNotBlank("key")
        log.debug { "Resolving bucket for key: $key" }
        // Keep prefix ownership in getBucketKey so overrides have one boundary.
        val bucketKey = validateBucketKeySize(getBucketKey(key))

        return proxyManager.builder()
            .build(bucketKey) { bucketConfiguration }
            .apply {
                log.debug { "Resolved bucket for key[$key] with prefix[$keyPrefix]" }
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
