package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.toUtf8Bytes
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.distributed.proxy.ProxyManager

/**
 * Bucket4j Bucket을 Redis 서버에 저장하고, 특정 Key 기반의 Rate-limit을 수행하는 Bucket 을 제공합니다.
 * 보통은 IP Address 기반이지만, User 기반으로 Rate-limit을 적용할 수 있습니다.
 *
 * ```
 * class UserBasedBucketProvider(
 *    proxyManager: ProxyManager<ByteArray>,
 *    bucketConfiguration: BucketConfiguration,
 *    keyPrefix: String
 * ): BucketProxyProvider(proxyManager, bucketConfiguration, keyPrefix) {
 *
 *     companion object: KLogging()
 *
 *     override fun getBucketKey(key: String): ByteArray {
 *          return "$tokenPrefix$key".toUtf8Bytes()
 *     }
 * }
 * ```
 *
 * @property proxyManager Bucket4j [ProxyManager] 인스턴스 (@see Bucket4jConfig)
 * @property bucketConfiguration Bucket Configuration
 * @property keyPrefix Bucket Key Prefix
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
     * Key 기반의 Bucket을 [ProxyManager]로 부터 가져온다
     *
     * @param key Bucket 소유자 (Rate Limit 적용 대상) Key
     * @return [Bucket] 인스턴스
     */
    fun resolveBucket(key: String): BucketProxy {
        key.requireNotBlank("key")
        log.debug { "Resolving bucket for key: $key" }
        // Prefix는 getBucketKey 에서 단일 책임으로 처리한다.
        val bucketKey = getBucketKey(key)

        return proxyManager.builder()
            .build(bucketKey) { bucketConfiguration }
            .apply {
                log.debug { "Resolved bucket for key[$key]: avaiableTokens=${this.availableTokens}" }
            }
    }

    protected open fun getBucketKey(key: String): ByteArray {
        return "$keyPrefix$key".toUtf8Bytes()
    }
}
