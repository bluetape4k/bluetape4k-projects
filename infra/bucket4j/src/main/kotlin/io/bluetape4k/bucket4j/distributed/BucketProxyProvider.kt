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
 * 원격 저장소 기반 [BucketProxy]를 key별로 조회하는 provider 입니다.
 *
 * ## 동작/계약
 * - [resolveBucket]은 blank key를 허용하지 않습니다.
 * - 실제 원격 bucket key는 [keyPrefix] + `key`를 UTF-8 바이트 배열로 직렬화해 구성합니다.
 * - resolve 시점에는 bucket 생성/조회만 수행하고, 잔여 토큰 조회 같은 추가 원격 호출은 하지 않습니다.
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
 * @property proxyManager Bucket4j [ProxyManager] 인스턴스
 * @property bucketConfiguration Bucket Configuration
 * @property keyPrefix Bucket Key Prefix. Redis namespace 충돌 방지를 위해 기본 prefix가 적용됩니다.
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
     * [key]에 해당하는 [BucketProxy]를 반환합니다.
     *
     * ## 동작/계약
     * - [key]는 blank일 수 없습니다.
     * - 반환값은 같은 key에 대해 동일한 원격 상태를 바라봅니다.
     * - 토큰 잔량 조회는 호출자가 명시적으로 수행해야 하며, 이 메서드는 resolve만 담당합니다.
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
                log.debug { "Resolved bucket for key[$key] with prefix[$keyPrefix]" }
            }
    }

    /**
     * 실제 원격 저장소에 사용할 bucket key를 생성합니다.
     *
     * 기본 구현은 [keyPrefix]를 붙인 뒤 UTF-8 바이트 배열로 변환합니다.
     */
    protected open fun getBucketKey(key: String): ByteArray {
        return "$keyPrefix$key".toUtf8Bytes()
    }
}
