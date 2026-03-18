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

object HazelcastSuspendNearJCache: KLogging() {

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
