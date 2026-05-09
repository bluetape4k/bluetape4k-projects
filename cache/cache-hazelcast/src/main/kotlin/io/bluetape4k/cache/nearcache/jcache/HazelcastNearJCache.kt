package io.bluetape4k.cache.nearcache.jcache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCacheEntryEventListener
import io.bluetape4k.cache.jcache.getDefaultJCacheConfiguration
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableCacheEntryListenerConfiguration

/**
 * Hazelcast JCache 기반 [NearJCache] 팩토리 오브젝트입니다.
 *
 * Hazelcast ICache 백엔드와 프론트 캐시 간 이벤트 기반 동기화를 수행합니다.
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
     * @param frontCache 프론트 JCache (로컬 캐시)
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param configuration JCache 설정
     * @param nearCacheCfg [NearJCacheConfig] 설정
     * @return 이벤트 리스너가 등록된 [NearJCache] 인스턴스
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        frontCache: JCache<K, V>,
        hazelcastInstance: HazelcastInstance,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        nearCacheCfg: NearJCacheConfig<K, V>,
    ): NearJCache<K, V> {
        // back cache의 event를 받아 front cache에 반영합니다.
        val cacheEntryEventListenerCfg =
            MutableCacheEntryListenerConfiguration(
                { JCacheEntryEventListener(frontCache) },
                null,
                false,
                nearCacheCfg.isSynchronous
            )

        val backCache: JCache<K, V> =
            HazelcastJCaching.getOrCreate(hazelcastInstance, nearCacheCfg.cacheName, configuration)
        log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$cacheEntryEventListenerCfg" }
        backCache.registerCacheEntryListener(cacheEntryEventListenerCfg)

        log.info { "Create NearCache instance. config=$nearCacheCfg" }
        return NearJCache(frontCache, backCache, nearCacheCfg)
    }
}
