package io.bluetape4k.cache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.HazelcastSuspendJCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.hazelcastNearCacheConfig
import io.bluetape4k.logging.KLogging
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast 기반 캐시 인스턴스를 생성하는 팩토리 오브젝트입니다.
 *
 * [JCache], [SuspendJCache], [HazelcastNearCache], [HazelcastSuspendNearCache]를 편리하게 생성할 수 있습니다.
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
        hazelcastInstance: HazelcastInstance,
        cacheName: String,
        configuration: Configuration<K, V> =
            MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
    ): JCache<K, V> = HazelcastJCaching.getOrCreate(hazelcastInstance, cacheName, configuration)

    // ─────────────────────────────────────────────
    // SuspendCache
    // ─────────────────────────────────────────────

    /**
     * 이름으로 Hazelcast [SuspendJCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @return [HazelcastSuspendJCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> suspendCache(
        hazelcastInstance: HazelcastInstance,
        cacheName: String,
    ): HazelcastSuspendJCache<K, V> = HazelcastSuspendJCache(hazelcastInstance, cacheName)

    /**
     * 이름과 설정으로 Hazelcast [SuspendJCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정
     * @return [HazelcastSuspendJCache] 인스턴스
     */
    fun <K : Any, V : Any> suspendCache(
        hazelcastInstance: HazelcastInstance,
        cacheName: String,
        configuration: Configuration<K, V>,
    ): HazelcastSuspendJCache<K, V> = HazelcastSuspendJCache(hazelcastInstance, cacheName, configuration)

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
    ): NearCacheOperations<V> = HazelcastNearCache(hazelcastInstance, config)

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
    ): NearCacheOperations<V> = nearCache(hazelcastInstance, hazelcastNearCacheConfig(block))

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
    ): SuspendNearCacheOperations<V> = HazelcastSuspendNearCache(hazelcastInstance, config)

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
    ): SuspendNearCacheOperations<V> = suspendNearCache(hazelcastInstance, hazelcastNearCacheConfig(block))
}
