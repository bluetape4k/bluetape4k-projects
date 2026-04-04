package io.bluetape4k.cache.nearcache

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.time.Duration

/**
 * Hazelcast IMap 기반 Near Cache 설정.
 *
 * JCache를 우회하고 Hazelcast 네이티브 [com.hazelcast.map.IMap] API를 사용한다.
 * [com.hazelcast.map.IMap.addEntryListener]는 클라이언트 측에서 실행되어 직렬화가 불필요하다.
 *
 * ```kotlin
 * val config = HazelcastNearCacheConfig(
 *     cacheName = "users",
 *     maxLocalSize = 5_000,
 *     frontExpireAfterWrite = Duration.ofMinutes(10),
 *     recordStats = true
 * )
 * // config.cacheName == "users"
 * ```
 *
 * @param cacheName 캐시(IMap) 이름
 * @param maxLocalSize 로컬(Caffeine) 캐시 최대 크기
 * @param frontExpireAfterWrite 로컬 캐시 쓰기 후 만료 시간. 0보다 커야 한다.
 * @param frontExpireAfterAccess 로컬 캐시 접근 후 만료 시간 (null이면 비활성). 지정하면 0보다 커야 한다.
 * @param recordStats 로컬 캐시 통계 기록 여부
 */
data class HazelcastNearCacheConfig(
    val cacheName: String = "hazelcast-near-cache",
    val maxLocalSize: Long = 10_000,
    val frontExpireAfterWrite: Duration = Duration.ofMinutes(30),
    val frontExpireAfterAccess: Duration? = null,
    val recordStats: Boolean = false,
) {
    init {
        cacheName.requireNotBlank("cacheName")
        maxLocalSize.requirePositiveNumber("maxLocalSize")
        frontExpireAfterWrite.requireGt(Duration.ZERO, "frontExpireAfterWrite")
        frontExpireAfterAccess?.requireGt(Duration.ZERO, "frontExpireAfterAccess")
    }
}

/**
 * [HazelcastNearCacheConfig] DSL 빌더.
 *
 * ```kotlin
 * val config = hazelcastNearCacheConfig {
 *     cacheName = "products"
 *     maxLocalSize = 1_000
 *     frontExpireAfterWrite = Duration.ofMinutes(5)
 *     recordStats = true
 * }
 * // config.cacheName == "products"
 * ```
 *
 * @param block [HazelcastNearCacheConfigBuilder] DSL 블록
 * @return 빌드된 [HazelcastNearCacheConfig] 인스턴스
 */
inline fun hazelcastNearCacheConfig(
    block: HazelcastNearCacheConfigBuilder.() -> Unit,
): HazelcastNearCacheConfig =
    HazelcastNearCacheConfigBuilder().apply(block).build()

/**
 * [HazelcastNearCacheConfig] 빌더 클래스.
 *
 * DSL 방식으로 [HazelcastNearCacheConfig]를 구성할 때 사용합니다.
 * 직접 인스턴스를 생성하기보다 [hazelcastNearCacheConfig] 함수를 사용하세요.
 *
 * ```kotlin
 * val builder = HazelcastNearCacheConfigBuilder().apply {
 *     cacheName = "orders"
 *     maxLocalSize = 2_000
 * }
 * val config = builder.build()
 * // config.cacheName == "orders"
 * ```
 */
class HazelcastNearCacheConfigBuilder {
    var cacheName: String = "hazelcast-near-cache"
    var maxLocalSize: Long = 10_000
    var frontExpireAfterWrite: Duration = Duration.ofMinutes(30)
    var frontExpireAfterAccess: Duration? = null
    var recordStats: Boolean = false

    /**
     * 설정값을 검증하고 [HazelcastNearCacheConfig]를 생성합니다.
     *
     * ```kotlin
     * val config = HazelcastNearCacheConfigBuilder().apply {
     *     cacheName = "catalog"
     *     maxLocalSize = 500
     * }.build()
     * // config.cacheName == "catalog"
     * ```
     *
     * @return 빌드된 [HazelcastNearCacheConfig] 인스턴스
     * @throws IllegalArgumentException cacheName이 blank이거나 maxLocalSize가 0 이하인 경우
     */
    fun build(): HazelcastNearCacheConfig = HazelcastNearCacheConfig(
        cacheName = cacheName.requireNotBlank("cacheName"),
        maxLocalSize = maxLocalSize.requirePositiveNumber("maxLocalSize"),
        frontExpireAfterWrite = frontExpireAfterWrite,
        frontExpireAfterAccess = frontExpireAfterAccess,
        recordStats = recordStats,
    )
}
