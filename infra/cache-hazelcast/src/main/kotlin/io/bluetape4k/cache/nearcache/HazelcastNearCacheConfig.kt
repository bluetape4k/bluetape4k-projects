package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * Hazelcast IMap 기반 Near Cache 설정.
 *
 * JCache를 우회하고 Hazelcast 네이티브 [com.hazelcast.map.IMap] API를 사용한다.
 * [com.hazelcast.map.IMap.addEntryListener]는 클라이언트 측에서 실행되어 직렬화가 불필요하다.
 *
 * @param cacheName 캐시(IMap) 이름
 * @param maxLocalSize 로컬(Caffeine) 캐시 최대 크기
 * @param frontExpireAfterWrite 로컬 캐시 쓰기 후 만료 시간
 * @param frontExpireAfterAccess 로컬 캐시 접근 후 만료 시간 (null이면 비활성)
 * @param recordStats 로컬 캐시 통계 기록 여부
 */
data class HazelcastNearCacheConfig(
    val cacheName: String = "hazelcast-near-cache",
    val maxLocalSize: Long = 10_000,
    val frontExpireAfterWrite: Duration = Duration.ofMinutes(30),
    val frontExpireAfterAccess: Duration? = null,
    val recordStats: Boolean = false,
)

/**
 * [HazelcastNearCacheConfig] DSL 빌더.
 */
inline fun hazelcastNearCacheConfig(
    block: HazelcastNearCacheConfigBuilder.() -> Unit,
): HazelcastNearCacheConfig =
    HazelcastNearCacheConfigBuilder().apply(block).build()

/**
 * [HazelcastNearCacheConfig] 빌더 클래스.
 */
class HazelcastNearCacheConfigBuilder {
    var cacheName: String = "hazelcast-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var recordStats: Boolean = false

    fun build(): HazelcastNearCacheConfig = HazelcastNearCacheConfig(
        cacheName = cacheName.requireNotBlank("cacheName"),
        maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
        frontExpireAfterWrite = frontExpireAfterWrite,
        frontExpireAfterAccess = frontExpireAfterAccess,
        recordStats = recordStats,
    )
}
