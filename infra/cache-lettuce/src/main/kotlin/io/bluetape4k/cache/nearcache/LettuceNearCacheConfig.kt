package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * Lettuce Near Cache (2-tier cache) 설정.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
data class NearCacheConfig<K: Any, V: Any>(
    val cacheName: String = "lettuce-near-cache",
    val maxLocalSize: Long = 10_000,
    val frontExpireAfterWrite: Duration = Duration.ofMinutes(30),
    val frontExpireAfterAccess: Duration? = null,
    val redisTtl: Duration? = null,
    val useRespProtocol3: Boolean = true,
    val recordStats: Boolean = false,
) {
    init {
        require(cacheName.isNotBlank()) { "cacheName must not be blank" }
        require(':' !in cacheName) {
            "cacheName must not contain ':' to avoid Redis key prefix collision, but was: '$cacheName'. " +
                "Use '-' or '_' as separator instead (e.g. 'my-cache', 'cache_v2')."
        }
    }

    /**
     * cacheName prefix를 포함한 Redis key를 생성한다.
     * 예: cacheName="orders", key="user:123" → "orders:user:123"
     *
     * key에는 ':'를 포함할 수 있다. invalidation 수신 시 startsWith + removePrefix 방식으로
     * cacheName prefix만 제거하므로 key의 ':' 문자는 그대로 보존된다.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun redisKey(key: String): String = "${cacheName}:${key}"
}

/**
 * [NearCacheConfig] DSL 빌더.
 */
inline fun <K: Any, V: Any> nearCacheConfig(
    @BuilderInference block: NearCacheConfigBuilder<K, V>.() -> Unit,
): NearCacheConfig<K, V> =
    NearCacheConfigBuilder<K, V>().apply(block).build()

class NearCacheConfigBuilder<K: Any, V: Any> {
    var cacheName: String = "lettuce-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var redisTtl: Duration? = null
    var useRespProtocol3: Boolean = true
    var recordStats: Boolean = false

    fun build(): NearCacheConfig<K, V> = NearCacheConfig(
        cacheName = cacheName.requireNotBlank("cacheName"),
        maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
        frontExpireAfterWrite = frontExpireAfterWrite,
        frontExpireAfterAccess = frontExpireAfterAccess,
        redisTtl = redisTtl,
        useRespProtocol3 = useRespProtocol3,
        recordStats = recordStats,
    )
}
