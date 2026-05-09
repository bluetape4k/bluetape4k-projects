package io.bluetape4k.cache.nearcache

/**
 * NearCache 통계 인터페이스.
 *
 * 로컬 캐시(front)와 백엔드 캐시(back)의 hit/miss 통계를 제공합니다.
 * 구현체는 Caffeine [com.github.benmanes.caffeine.cache.stats.CacheStats]에서
 * 로컬 통계를 매핑하고, [java.util.concurrent.atomic.AtomicLong] 카운터로
 * 백엔드 통계를 추적합니다.
 *
 * ```kotlin
 * val cache: NearCacheOperations<String> = lettuceNearCacheOf(redisClient, codec, config)
 * cache.put("hello", "world")
 * val value = cache.get("hello")
 * val stats: NearCacheStatistics = cache.stats()
 * // stats.localHits >= 0
 * // stats.hitRate between 0.0 and 1.0
 * ```
 */
interface NearCacheStatistics {
    /** 로컬 캐시 히트 수 */
    val localHits: Long

    /** 로컬 캐시 미스 수 */
    val localMisses: Long

    /** 로컬 캐시 현재 엔트리 수 */
    val localSize: Long

    /** 로컬 캐시 퇴거 수 */
    val localEvictions: Long

    /** 백엔드 캐시 히트 수 (로컬 미스 후 백엔드에서 찾은 경우) */
    val backHits: Long

    /** 백엔드 캐시 미스 수 (로컬, 백엔드 모두 없는 경우) */
    val backMisses: Long

    /** 전체 히트율: (localHits + backHits) / (localHits + backHits + backMisses) */
    val hitRate: Double
}

/**
 * [NearCacheStatistics]의 기본 구현체.
 *
 * 불변 스냅샷으로, 통계 조회 시점의 값을 캡처합니다.
 *
 * ```kotlin
 * val stats = DefaultNearCacheStatistics(
 *     localHits = 10, localMisses = 2, localSize = 8,
 *     backHits = 1, backMisses = 1
 * )
 * // stats.hitRate == (10 + 1).toDouble() / (10 + 1 + 1) == 0.916...
 * ```
 */
data class DefaultNearCacheStatistics(
    override val localHits: Long = 0,
    override val localMisses: Long = 0,
    override val localSize: Long = 0,
    override val localEvictions: Long = 0,
    override val backHits: Long = 0,
    override val backMisses: Long = 0,
): NearCacheStatistics {
    override val hitRate: Double
        get() {
            val total = localHits + backHits + backMisses
            return if (total == 0L) 0.0 else (localHits + backHits).toDouble() / total
        }
}
