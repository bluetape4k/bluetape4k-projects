package io.bluetape4k.bucket4j.distributed

import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.AsyncProxyManager

/**
 * Bucket4j Bucket을 Redis 서버에 저장하고, 특정 Key 기반의 Rate-limit을 수행하는 Bucket 을 제공합니다.
 * 보통은 IP Address 기반이지만, User 기반으로 Rate-limit을 적용할 수 있습니다.
 *
 * ```
 * class UserBasedAsyncBucketProvider(
 *    asyncProxyManager: AsyncProxyManager<ByteArray>,
 *    bucketConfiguration: BucketConfiguration,
 *    tokenPrefix: String
 * ): BucketProxyProvider(proxyManager, bucketConfiguration, tokenPrefix) {
 *
 *     companion object: KLogging()
 *
 *     override fun getBucketKey(key: String): ByteArray {
 *          return "$tokenPrefix$key".toUtf8Bytes()
 *     }
 * }
 * ```
 *
 * @property asyncProxyManager Bucket4j [AsyncProxyManager] 인스턴스 (@see Bucket4jConfig)
 * @property bucketConfiguration Bucket Configuration
 * @property keyPrefix Bucket Key Prefix
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
     * Key 기반의 [AsyncBucketProxy]를 [AsyncProxyManager]로 부터 가져온다
     *
     * @param key Bucket 소유자 (Rate Limit 적용 대상) Key
     * @return [Bucket] 인스턴스
     */
    fun resolveBucket(key: String): AsyncBucketProxy {
        log.debug { "Resolving AsyncBucketProxy for key: $key" }
        val bucketKey = getBucketKey("$keyPrefix$key")

        return asyncProxyManager.builder()
            .build(bucketKey) { completableFutureOf(bucketConfiguration) }
            .apply {
                log.debug { "Resolved bucket for key[$key]: avaiableTokens=${this.availableTokens.get()}" }
            }
    }

    protected open fun getBucketKey(key: String): ByteArray {
        return "$keyPrefix$key".toUtf8Bytes()
    }

}
