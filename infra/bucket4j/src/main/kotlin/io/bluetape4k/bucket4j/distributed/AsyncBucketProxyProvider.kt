package io.bluetape4k.bucket4j.distributed

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
 * 비동기 원격 저장소 기반 [AsyncBucketProxy]를 key별로 조회하는 provider 입니다.
 *
 * ## 동작/계약
 * - [resolveBucket]은 blank key를 허용하지 않습니다.
 * - 실제 원격 bucket key는 [keyPrefix] + `key`를 UTF-8 바이트 배열로 직렬화해 구성합니다.
 * - resolve 시점에는 proxy 생성/조회만 수행하고, 잔여 토큰 조회 같은 추가 비동기 호출은 하지 않습니다.
 *
 * ```
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
 * @property asyncProxyManager Bucket4j [AsyncProxyManager] 인스턴스
 * @property bucketConfiguration Bucket Configuration
 * @property keyPrefix Bucket Key Prefix. Redis namespace 충돌 방지를 위해 기본 prefix가 적용됩니다.
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
     * [key]에 해당하는 [AsyncBucketProxy]를 반환합니다.
     *
     * ## 동작/계약
     * - [key]는 blank일 수 없습니다.
     * - 반환값은 같은 key에 대해 동일한 원격 상태를 바라봅니다.
     * - future completion과 토큰 잔량 조회는 호출자가 명시적으로 수행해야 합니다.
     *
     * @param key Bucket 소유자 (Rate Limit 적용 대상) Key
     * @return [Bucket] 인스턴스
     */
    fun resolveBucket(key: String): AsyncBucketProxy {
        key.requireNotBlank("key")
        log.debug { "Resolving AsyncBucketProxy for key: $key" }
        // Prefix는 getBucketKey 에서 단일 책임으로 처리한다.
        val bucketKey = getBucketKey(key)

        return asyncProxyManager.builder()
            .build(bucketKey) { completableFutureOf(bucketConfiguration) }
            .apply {
                log.debug { "Resolved async bucket for key[$key] with prefix[$keyPrefix]" }
            }
    }

    /**
     * 실제 원격 저장소에 사용할 async bucket key를 생성합니다.
     *
     * 기본 구현은 [keyPrefix]를 붙인 뒤 UTF-8 바이트 배열로 변환합니다.
     */
    protected open fun getBucketKey(key: String): ByteArray {
        return "$keyPrefix$key".toUtf8Bytes()
    }

}
