package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.redisson.api.LocalCachedMapOptions
import java.time.Duration

/**
 * Redisson [RLocalCachedMap][org.redisson.api.RLocalCachedMap] 기반 Near Cache 설정.
 *
 * `RLocalCachedMap`은 Redisson 내장 2-tier 캐시로, 자동 로컬 캐시 + 분산 캐시 + invalidation을 제공합니다.
 * Lettuce RESP3 하이브리드 없이도 client-side caching이 동작합니다.
 *
 * @param cacheName 캐시 이름 (Redis map 이름으로 사용됨)
 * @param maxLocalSize 로컬 캐시 최대 항목 수
 * @param timeToLive Redis 저장 TTL (null이면 만료 없음)
 * @param maxIdle Redis idle 만료 시간 (null이면 비활성)
 * @param syncStrategy 로컬 캐시 동기화 전략
 * @param reconnectionStrategy 재연결 시 로컬 캐시 처리 전략
 * @param evictionPolicy 로컬 캐시 퇴거 정책
 */
data class RedissonNearCacheConfig(
    val cacheName: String = "redisson-near-cache",
    val maxLocalSize: Int = 10_000,
    val timeToLive: Duration? = null,
    val maxIdle: Duration? = null,
    val syncStrategy: LocalCachedMapOptions.SyncStrategy = LocalCachedMapOptions.SyncStrategy.INVALIDATE,
    val reconnectionStrategy: LocalCachedMapOptions.ReconnectionStrategy = LocalCachedMapOptions.ReconnectionStrategy.CLEAR,
    val evictionPolicy: LocalCachedMapOptions.EvictionPolicy = LocalCachedMapOptions.EvictionPolicy.LRU,
) {
    init {
        require(cacheName.isNotBlank()) { "cacheName은 비어 있으면 안 됩니다" }
        require(maxLocalSize > 0) { "maxLocalSize는 0보다 커야 합니다. 현재 값: $maxLocalSize" }
    }
}

/**
 * [RedissonNearCacheConfig] DSL 빌더 함수.
 *
 * ```kotlin
 * val config = redissonNearCacheConfig {
 *     cacheName = "my-cache"
 *     maxLocalSize = 5_000
 *     timeToLive = Duration.ofMinutes(10)
 *     syncStrategy = LocalCachedMapOptions.SyncStrategy.INVALIDATE
 * }
 * ```
 *
 * @param block [RedissonNearCacheConfigBuilder]에 대한 설정 블록
 * @return 빌드된 [RedissonNearCacheConfig] 인스턴스
 */
inline fun redissonNearCacheConfig(block: RedissonNearCacheConfigBuilder.() -> Unit): RedissonNearCacheConfig =
    RedissonNearCacheConfigBuilder().apply(block).build()

/**
 * [RedissonNearCacheConfig] 빌더 클래스.
 */
class RedissonNearCacheConfigBuilder {
    /** 캐시 이름 (Redis map 이름으로 사용됨). 기본값: `"redisson-near-cache"` */
    var cacheName: String = "redisson-near-cache"

    /** 로컬 캐시 최대 항목 수. 기본값: `10_000` */
    var maxLocalSize: Int = 10_000

    /** Redis 저장 TTL. `null`이면 만료 없음. 기본값: `null` */
    var timeToLive: Duration? = null

    /** Redis idle 만료 시간. `null`이면 비활성. 기본값: `null` */
    var maxIdle: Duration? = null

    /** 로컬 캐시 동기화 전략. 기본값: [LocalCachedMapOptions.SyncStrategy.INVALIDATE] */
    var syncStrategy: LocalCachedMapOptions.SyncStrategy = LocalCachedMapOptions.SyncStrategy.INVALIDATE

    /** 재연결 시 로컬 캐시 처리 전략. 기본값: [LocalCachedMapOptions.ReconnectionStrategy.CLEAR] */
    var reconnectionStrategy: LocalCachedMapOptions.ReconnectionStrategy = LocalCachedMapOptions.ReconnectionStrategy.CLEAR

    /** 로컬 캐시 퇴거 정책. 기본값: [LocalCachedMapOptions.EvictionPolicy.LRU] */
    var evictionPolicy: LocalCachedMapOptions.EvictionPolicy = LocalCachedMapOptions.EvictionPolicy.LRU

    /**
     * 설정값을 검증하고 [RedissonNearCacheConfig]를 생성합니다.
     *
     * @return 빌드된 [RedissonNearCacheConfig] 인스턴스
     * @throws IllegalArgumentException cacheName이 blank이거나 maxLocalSize가 0 이하인 경우
     */
    fun build(): RedissonNearCacheConfig =
        RedissonNearCacheConfig(
            cacheName = cacheName.requireNotBlank("cacheName"),
            maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
            timeToLive = timeToLive,
            maxIdle = maxIdle,
            syncStrategy = syncStrategy,
            reconnectionStrategy = reconnectionStrategy,
            evictionPolicy = evictionPolicy
        )
}
