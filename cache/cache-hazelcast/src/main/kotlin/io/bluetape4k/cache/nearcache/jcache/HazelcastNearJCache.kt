package io.bluetape4k.cache.nearcache.jcache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.getDefaultJCacheConfiguration
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import javax.cache.configuration.Configuration

/**
 * Hazelcast JCache 기반 [NearJCache] 팩토리 오브젝트입니다.
 *
 * Hazelcast JCache의 직렬화 경계에 맞춰 listener 없이 동작하는 degraded 모드의
 * [NearJCache] 팩토리입니다. read-through와 write-through는 제공하지만 peer
 * front-cache 전파는 보장하지 않습니다.
 *
 * ```kotlin
 * val nearCache = HazelcastNearJCache(
 *     frontCache = frontCache,
 *     hazelcastInstance = hazelcastInstance,
 *     nearCacheCfg = NearJCacheConfig(cacheName = "products")
 * )
 * nearCache.put("p1", "Widget")
 * val name = nearCache.get("p1")
 * // name == "Widget"
 * ```
 */
object HazelcastNearJCache: KLogging() {

    /**
     * 프론트 캐시 + Hazelcast 백엔드 캐시를 조합하여 [NearJCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastNearJCache<String, String>(
     *     frontCache = frontJCache,
     *     hazelcastInstance = hazelcastInstance,
     *     nearCacheCfg = NearJCacheConfig(cacheName = "data")
     * )
     * near.put("k", "v")
     * val value = near.get("k")
     * // value == "v"
     * ```
     *
     * @param frontCache 프론트 JCache (로컬 캐시). [NearJCache.close] 호출 시 이 캐시가 닫히며,
     * factory가 생성하거나 재사용한 Hazelcast back cache와 [HazelcastInstance]는 닫지 않습니다.
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param configuration JCache 설정
     * @param nearCacheCfg [NearJCacheConfig] 설정
     * @return listener가 등록되지 않은 degraded [NearJCache] 인스턴스
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        frontCache: JCache<K, V>,
        hazelcastInstance: HazelcastInstance,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        nearCacheCfg: NearJCacheConfig<K, V>,
    ): NearJCache<K, V> = invoke(
        frontCache = frontCache,
        hazelcastInstance = hazelcastInstance,
        configuration = configuration,
        nearCacheCfg = nearCacheCfg,
        clearAuthority = NearJCacheClearAuthority.DENY,
    )

    /**
     * caller가 명시한 clear authority를 사용하는 listener-free factory입니다.
     * 기존 overload는 [NearJCacheClearAuthority.DENY]를 사용합니다. 이 overload는
     * caller가 공급한 front cache를 wrapper `close()`에서 닫고, factory가 생성하거나
     * 재사용한 Hazelcast back cache와 [HazelcastInstance]는 닫지 않습니다.
     *
     * @param clearAuthority caller가 확인한 back namespace 권한
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        frontCache: JCache<K, V>,
        hazelcastInstance: HazelcastInstance,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        nearCacheCfg: NearJCacheConfig<K, V>,
        clearAuthority: NearJCacheClearAuthority,
    ): NearJCache<K, V> {
        val backCache: JCache<K, V> =
            HazelcastJCaching.getOrCreate(hazelcastInstance, nearCacheCfg.cacheName, configuration)

        log.info { "Create NearCache instance. config=$nearCacheCfg" }
        return NearJCache(frontCache, backCache, nearCacheCfg, clearAuthority)
    }
}
