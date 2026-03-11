package io.bluetape4k.cache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientHazelcastNearCache
import io.bluetape4k.cache.nearcache.ResilientHazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.ResilientHazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.hazelcastNearCacheConfig
import io.bluetape4k.logging.KLogging
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast 기반 캐시 인스턴스를 생성하는 팩토리 오브젝트입니다.
 *
 * [JCache], [SuspendCache], [HazelcastNearCache], [HazelcastSuspendNearCache],
 * [ResilientHazelcastNearCache], [ResilientHazelcastSuspendNearCache]를 편리하게 생성할 수 있습니다.
 *
 * ```kotlin
 * val cache = HazelcastCaches.jcache<String, String>("my-cache")
 * val near  = HazelcastCaches.nearCache<String>(hazelcast) { cacheName = "my-near" }
 * ```
 */
object HazelcastCaches : KLogging() {

    // ─────────────────────────────────────────────
    // JCache
    // ─────────────────────────────────────────────

    /**
     * 이름과 설정으로 Hazelcast [JCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정 (기본값: [MutableConfiguration])
     * @return [JCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> jcache(
        cacheName: String,
        configuration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
    ): JCache<K, V> = HazelcastJCaching.getOrCreate(cacheName, configuration)

    // ─────────────────────────────────────────────
    // SuspendCache
    // ─────────────────────────────────────────────

    /**
     * 이름으로 Hazelcast [SuspendCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @return [HazelcastSuspendCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> suspendCache(
        cacheName: String,
    ): HazelcastSuspendCache<K, V> = HazelcastSuspendCache(cacheName)

    /**
     * 이름과 설정으로 Hazelcast [SuspendCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정
     * @return [HazelcastSuspendCache] 인스턴스
     */
    fun <K : Any, V : Any> suspendCache(
        cacheName: String,
        configuration: Configuration<K, V>,
    ): HazelcastSuspendCache<K, V> = HazelcastSuspendCache(cacheName, configuration)

    // ─────────────────────────────────────────────
    // NearCache
    // ─────────────────────────────────────────────

    /**
     * [HazelcastNearCacheConfig]로 [HazelcastNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Near Cache 설정
     * @return [HazelcastNearCache] 인스턴스
     */
    fun <V : Any> nearCache(
        hazelcastInstance: HazelcastInstance,
        config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
    ): HazelcastNearCache<V> = HazelcastNearCache(hazelcastInstance, config)

    /**
     * DSL 블록으로 [HazelcastNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [HazelcastNearCacheConfigBuilder] DSL 블록
     * @return [HazelcastNearCache] 인스턴스
     */
    fun <V : Any> nearCache(
        hazelcastInstance: HazelcastInstance,
        block: HazelcastNearCacheConfigBuilder.() -> Unit,
    ): HazelcastNearCache<V> = nearCache(hazelcastInstance, hazelcastNearCacheConfig(block))

    /**
     * [HazelcastNearCacheConfig]로 [HazelcastSuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Near Cache 설정
     * @return [HazelcastSuspendNearCache] 인스턴스
     */
    fun <V : Any> suspendNearCache(
        hazelcastInstance: HazelcastInstance,
        config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
    ): HazelcastSuspendNearCache<V> = HazelcastSuspendNearCache(hazelcastInstance, config)

    /**
     * DSL 블록으로 [HazelcastSuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [HazelcastNearCacheConfigBuilder] DSL 블록
     * @return [HazelcastSuspendNearCache] 인스턴스
     */
    fun <V : Any> suspendNearCache(
        hazelcastInstance: HazelcastInstance,
        block: HazelcastNearCacheConfigBuilder.() -> Unit,
    ): HazelcastSuspendNearCache<V> = suspendNearCache(hazelcastInstance, hazelcastNearCacheConfig(block))

    // ─────────────────────────────────────────────
    // Resilient NearCache
    // ─────────────────────────────────────────────

    /**
     * [ResilientHazelcastNearCacheConfig]로 [ResilientHazelcastNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Resilient Near Cache 설정
     * @return [ResilientHazelcastNearCache] 인스턴스
     */
    fun <V : Any> resilientNearCache(
        hazelcastInstance: HazelcastInstance,
        config: ResilientHazelcastNearCacheConfig = ResilientHazelcastNearCacheConfig(HazelcastNearCacheConfig()),
    ): ResilientHazelcastNearCache<V> = ResilientHazelcastNearCache(hazelcastInstance, config)

    /**
     * [HazelcastNearCacheConfig]로 [ResilientHazelcastNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param nearCacheConfig Near Cache 기본 설정
     * @return [ResilientHazelcastNearCache] 인스턴스
     */
    fun <V : Any> resilientNearCache(
        hazelcastInstance: HazelcastInstance,
        nearCacheConfig: HazelcastNearCacheConfig,
    ): ResilientHazelcastNearCache<V> =
        ResilientHazelcastNearCache(hazelcastInstance, ResilientHazelcastNearCacheConfig(nearCacheConfig))

    /**
     * [ResilientHazelcastNearCacheConfig]로 [ResilientHazelcastSuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Resilient Near Cache 설정
     * @return [ResilientHazelcastSuspendNearCache] 인스턴스
     */
    fun <V : Any> resilientSuspendNearCache(
        hazelcastInstance: HazelcastInstance,
        config: ResilientHazelcastNearCacheConfig = ResilientHazelcastNearCacheConfig(HazelcastNearCacheConfig()),
    ): ResilientHazelcastSuspendNearCache<V> = ResilientHazelcastSuspendNearCache(hazelcastInstance, config)

    /**
     * [HazelcastNearCacheConfig]로 [ResilientHazelcastSuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param nearCacheConfig Near Cache 기본 설정
     * @return [ResilientHazelcastSuspendNearCache] 인스턴스
     */
    fun <V : Any> resilientSuspendNearCache(
        hazelcastInstance: HazelcastInstance,
        nearCacheConfig: HazelcastNearCacheConfig,
    ): ResilientHazelcastSuspendNearCache<V> =
        ResilientHazelcastSuspendNearCache(hazelcastInstance, ResilientHazelcastNearCacheConfig(nearCacheConfig))
}
