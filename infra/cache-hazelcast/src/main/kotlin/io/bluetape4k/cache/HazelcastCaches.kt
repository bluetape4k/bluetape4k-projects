package io.bluetape4k.cache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.HazelcastSuspendJCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.HazelcastNearCache
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.HazelcastNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.HazelcastSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.hazelcastNearCacheConfig
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBuilder
import io.bluetape4k.cache.nearcache.jcache.SuspendNearJCache
import io.bluetape4k.cache.nearcache.jcache.nearJCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.util.concurrent.TimeUnit
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
object HazelcastCaches: KLogging() {
    // ─────────────────────────────────────────────
    // JCache
    // ─────────────────────────────────────────────

    /**
     * 이름과 설정으로 Hazelcast [JCache]를 생성하거나 재사용합니다.
     *
     * ```kotlin
     * val cache = HazelcastCaches.jcache<String, String>(hazelcastInstance, "my-cache")
     * cache.put("k", "v")
     * val value = cache.get("k")
     * // value == "v"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정 (기본값: [MutableConfiguration])
     * @return [JCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> jcache(
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
     * ```kotlin
     * val cache = HazelcastCaches.suspendJCache<String, String>(hazelcastInstance, "users")
     * cache.put("u1", "Alice")
     * val value = cache.get("u1")
     * // value == "Alice"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param cacheName 캐시 이름
     * @return [HazelcastSuspendJCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> suspendJCache(
        hazelcastInstance: HazelcastInstance,
        cacheName: String,
    ): HazelcastSuspendJCache<K, V> = HazelcastSuspendJCache(hazelcastInstance, cacheName)

    /**
     * 이름과 설정으로 Hazelcast [SuspendJCache]를 생성하거나 재사용합니다.
     *
     * ```kotlin
     * val config = MutableConfiguration<String, String>().apply { setTypes(String::class.java, String::class.java) }
     * val cache = HazelcastCaches.suspendJCache(hazelcastInstance, "scores", config)
     * cache.put("u1", "100")
     * val score = cache.get("u1")
     * // score == "100"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정
     * @return [HazelcastSuspendJCache] 인스턴스
     */
    fun <K: Any, V: Any> suspendJCache(
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
     * ```kotlin
     * val config = HazelcastNearCacheConfig(cacheName = "products")
     * val near = HazelcastCaches.nearCache<String>(hazelcastInstance, config)
     * near.put("p1", "Widget")
     * val name = near.get("p1")
     * // name == "Widget"
     * ```
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Near Cache 설정
     * @return [HazelcastNearCache] 인스턴스
     */
    fun <V: Any> nearCache(
        hazelcastInstance: HazelcastInstance,
        config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
    ): NearCacheOperations<V> = HazelcastNearCache(hazelcastInstance, config)

    /**
     * DSL 블록으로 [HazelcastNearCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastCaches.nearCache<String>(hazelcastInstance) {
     *     cacheName = "orders"
     *     maxLocalSize = 2_000
     * }
     * near.put("o1", "pending")
     * val status = near.get("o1")
     * // status == "pending"
     * ```
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [HazelcastNearCacheConfigBuilder] DSL 블록
     * @return [HazelcastNearCache] 인스턴스
     */
    fun <V: Any> nearCache(
        hazelcastInstance: HazelcastInstance,
        block: HazelcastNearCacheConfigBuilder.() -> Unit,
    ): NearCacheOperations<V> = nearCache(hazelcastInstance, hazelcastNearCacheConfig(block))

    /**
     * [HazelcastNearCacheConfig]로 [HazelcastSuspendNearCache]를 생성합니다.
     *
     * ```kotlin
     * val config = HazelcastNearCacheConfig(cacheName = "sessions")
     * val near = HazelcastCaches.suspendNearCache<String>(hazelcastInstance, config)
     * near.put("s1", "token")
     * val token = near.get("s1")
     * // token == "token"
     * ```
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config Near Cache 설정
     * @return [HazelcastSuspendNearCache] 인스턴스
     */
    fun <V: Any> suspendNearCache(
        hazelcastInstance: HazelcastInstance,
        config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
    ): SuspendNearCacheOperations<V> = HazelcastSuspendNearCache(hazelcastInstance, config)

    /**
     * DSL 블록으로 [HazelcastSuspendNearCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastCaches.suspendNearCache<String>(hazelcastInstance) {
     *     cacheName = "inventory"
     *     maxLocalSize = 3_000
     * }
     * near.put("i1", "in-stock")
     * val status = near.get("i1")
     * // status == "in-stock"
     * ```
     *
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [HazelcastNearCacheConfigBuilder] DSL 블록
     * @return [HazelcastSuspendNearCache] 인스턴스
     */
    fun <V: Any> suspendNearCache(
        hazelcastInstance: HazelcastInstance,
        block: HazelcastNearCacheConfigBuilder.() -> Unit,
    ): SuspendNearCacheOperations<V> = suspendNearCache(hazelcastInstance, hazelcastNearCacheConfig(block))

    // -------------------------------------
    // NearJCache
    // -------------------------------------

    /**
     * DSL 블록으로 Hazelcast 기반 [NearJCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastCaches.nearJCache<String, String>(hazelcastInstance) {
     *     cacheName = "catalog"
     * }
     * near.put("c1", "Widget")
     * val name = near.get("c1")
     * // name == "Widget"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [NearJCacheConfigBuilder] DSL 블록
     * @return [NearJCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> nearJCache(
        hazelcastInstance: HazelcastInstance,
        block: NearJCacheConfigBuilder<K, V>.() -> Unit,
    ): NearJCache<K, V> {
        val config = nearJCacheConfig(block)
        return nearJCache(hazelcastInstance, config)
    }

    /**
     * [NearJCacheConfig]로 Hazelcast 기반 [NearJCache]를 생성합니다.
     *
     * ```kotlin
     * val config = NearJCacheConfig<String, String>(cacheName = "catalog")
     * val near = HazelcastCaches.nearJCache<String, String>(hazelcastInstance, config)
     * near.put("c1", "Widget")
     * val name = near.get("c1")
     * // name == "Widget"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config [NearJCacheConfig] 설정
     * @return [NearJCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> nearJCache(
        hazelcastInstance: HazelcastInstance,
        config: NearJCacheConfig<K, V>,
    ): NearJCache<K, V> {
        val configuration = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        }
        val backCache: JCache<K, V> = HazelcastJCaching.getOrCreate(
            hazelcastInstance,
            config.cacheName,
            configuration,
        )
        // Hazelcast client JCache는 직렬화 불가 listener를 클러스터에 전파할 수 없으므로
        // NearJCache.invoke(config, backCache) 대신 front cache를 직접 생성하여 listener 없이 구성합니다.
        val frontCacheManager = config.cacheManagerFactory.create()
        val frontCache: JCache<K, V> =
            frontCacheManager.createCache(config.cacheName, config.frontCacheConfiguration)

        log.info { "NearJCache 생성. config=$config" }
        return NearJCache(frontCache, backCache, config)
    }

    /**
     * DSL 블록으로 Hazelcast 기반 [SuspendNearJCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastCaches.suspendNearJCache<String, String>(hazelcastInstance) {
     *     cacheName = "sessions"
     * }
     * near.put("s1", "token-abc")
     * val token = near.get("s1")
     * // token == "token-abc"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param block [NearJCacheConfigBuilder] DSL 블록
     * @return [SuspendNearJCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> suspendNearJCache(
        hazelcastInstance: HazelcastInstance,
        block: NearJCacheConfigBuilder<K, V>.() -> Unit,
    ): SuspendNearJCache<K, V> {
        val config = nearJCacheConfig(block)
        return suspendNearJCache(hazelcastInstance, config)
    }

    /**
     * [NearJCacheConfig]로 Hazelcast 기반 [SuspendNearJCache]를 생성합니다.
     *
     * ```kotlin
     * val config = NearJCacheConfig<String, String>(cacheName = "sessions")
     * val near = HazelcastCaches.suspendNearJCache<String, String>(hazelcastInstance, config)
     * near.put("s1", "token-xyz")
     * val token = near.get("s1")
     * // token == "token-xyz"
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param hazelcastInstance Hazelcast 인스턴스
     * @param config [NearJCacheConfig] 설정
     * @return [SuspendNearJCache] 인스턴스
     */
    inline fun <reified K: Any, reified V: Any> suspendNearJCache(
        hazelcastInstance: HazelcastInstance,
        config: NearJCacheConfig<K, V>,
    ): SuspendNearJCache<K, V> {
        val configuration = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        }
        val backJCache: JCache<K, V> = HazelcastJCaching.getOrCreate(
            hazelcastInstance,
            config.cacheName,
            configuration,
        )
        val backCache = HazelcastSuspendJCache(backJCache)

        val frontCache = CaffeineSuspendJCache<K, V> {
            maximumSize(10_000)
            expireAfterAccess(30, TimeUnit.MINUTES)
        }

        // Hazelcast client JCache는 listener를 클러스터에 직렬화해서 전파하므로
        // non-serializable front cache를 listener로 등록하면 실패합니다.
        log.info { "SuspendNearJCache 생성. config=$config" }
        return SuspendNearJCache.withoutListener(frontCache, backCache)
    }

}
