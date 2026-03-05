package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * Redisson + Lettuce RESP3 하이브리드 Near Cache (2-tier cache) 설정.
 *
 * 데이터 연산은 Redisson [RBucket][org.redisson.api.RBucket]을 사용하고,
 * invalidation은 Lettuce RESP3 CLIENT TRACKING push를 사용한다.
 *
 * @param cacheName 캐시 이름 (':'를 포함하면 안 됨, Redis key prefix로 사용됨)
 * @param maxLocalSize 로컬 캐시 최대 크기
 * @param frontExpireAfterWrite 로컬 캐시 쓰기 만료 시간
 * @param frontExpireAfterAccess 로컬 캐시 접근 만료 시간 (null이면 비활성)
 * @param redisTtl Redis 저장 TTL (null이면 만료 없음)
 * @param useRespProtocol3 RESP3 CLIENT TRACKING 활성화 여부
 * @param recordStats 로컬 캐시 통계 기록 여부
 */
data class RedissonResp3NearCacheConfig(
    val cacheName: String = "redisson-near-cache",
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
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun redisKey(key: String): String = "${cacheName}:${key}"
}

/**
 * [RedissonResp3NearCacheConfig] DSL 빌더.
 */
inline fun redissonResp3NearCacheConfig(
    block: RedissonResp3NearCacheConfigBuilder.() -> Unit,
): RedissonResp3NearCacheConfig =
    RedissonResp3NearCacheConfigBuilder().apply(block).build()

/**
 * [RedissonResp3NearCacheConfig] 빌더 클래스.
 */
class RedissonResp3NearCacheConfigBuilder {
    var cacheName: String = "redisson-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var redisTtl: Duration? = null
    var useRespProtocol3: Boolean = true
    var recordStats: Boolean = false

    fun build(): RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(
        cacheName = cacheName.requireNotBlank("cacheName"),
        maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
        frontExpireAfterWrite = frontExpireAfterWrite,
        frontExpireAfterAccess = frontExpireAfterAccess,
        redisTtl = redisTtl,
        useRespProtocol3 = useRespProtocol3,
        recordStats = recordStats,
    )
}
