package io.bluetape4k.cache.nearcache.jcache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.cache.jcache.HazelcastJCaching
import io.bluetape4k.cache.jcache.HazelcastSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListener
import io.bluetape4k.cache.jcache.getDefaultJCacheConfiguration
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableCacheEntryListenerConfiguration

/**
 * Hazelcast JCache 기반 [SuspendNearJCache] 팩토리 오브젝트입니다.
 *
 * 코루틴 환경에서 Hazelcast 백엔드를 사용하는 NearCache를 생성합니다.
 *
 * ```kotlin
 * val near = HazelcastSuspendNearJCache(
 *     frontCache = suspendFrontCache,
 *     hazelcastInstance = hazelcastInstance,
 *     nearCacheCfg = NearJCacheConfig(cacheName = "sessions")
 * )
 * near.put("s1", "token")
 * val token = near.get("s1")
 * // token == "token"
 * ```
 */
object HazelcastSuspendNearJCache: KLogging() {

    /**
     * 프론트 SuspendCache + Hazelcast 백엔드 캐시를 조합하여 [SuspendNearJCache]를 생성합니다.
     *
     * ```kotlin
     * val near = HazelcastSuspendNearJCache<String, String>(
     *     frontCache = suspendFrontCache,
     *     hazelcastInstance = hazelcastInstance,
     *     nearCacheCfg = NearJCacheConfig(cacheName = "orders")
     * )
     * near.put("o1", "pending")
     * val status = near.get("o1")
     * // status == "pending"
     * ```
     *
     * @param frontCache 프론트 [SuspendJCache] (로컬 캐시)
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param configuration JCache 설정
     * @param nearCacheCfg [NearJCacheConfig] 설정
     * @return 이벤트 리스너가 등록된 [SuspendNearJCache] 인스턴스
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        frontCache: SuspendJCache<K, V>,
        hazelcastInstance: HazelcastInstance,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        nearCacheCfg: NearJCacheConfig<K, V>,
    ): SuspendNearJCache<K, V> {
        val cacheEntryEventListenerCfg = MutableCacheEntryListenerConfiguration(
            { SuspendJCacheEntryEventListener(frontCache) },
            null,
            false,
            false
        )

        val jcache = HazelcastJCaching.getOrCreate(hazelcastInstance, nearCacheCfg.cacheName, configuration)
        val backCache = HazelcastSuspendJCache(jcache)

        log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$cacheEntryEventListenerCfg" }
        backCache.registerCacheEntryListener(cacheEntryEventListenerCfg)

        log.info { "Create HazelcastSuspendNearJCache instance." }
        return SuspendNearJCache(frontCache, backCache)
    }
}
