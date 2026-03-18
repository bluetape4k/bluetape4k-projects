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

object HazelcastNearJCache: KLogging() {

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
