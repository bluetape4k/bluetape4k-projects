package io.bluetape4k.cache.jcache

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import javax.cache.CacheManager
import javax.cache.configuration.Configuration

/**
 * Lettuce를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object입니다.
 *
 * ```kotlin
 * val cache = LettuceJCaching.getOrCreate<String, String>("my-cache")
 * cache.put("key", "value")
 * ```
 */
object LettuceJCaching: KLogging() {

    /**
     * Lettuce 기반 [CacheManager]
     */
    val cacheManager: CacheManager by lazy { jcacheManager<LettuceCachingProvider>() }

    /**
     * [JCache]`<K, V>`를 생성하거나 기존 캐시를 가져옵니다.
     *
     * @param cacheName 캐시 이름
     * @param ttlSeconds TTL (초), null이면 만료 없음
     * @param serializer 직렬화 방식 (기본값: Fory)
     */
    inline fun <reified K: Any, reified V: Any> getOrCreate(
        cacheName: String,
        ttlSeconds: Long? = null,
        serializer: BinarySerializer = BinarySerializers.Fory,
    ): JCache<K, V> {
        val manager = cacheManager as? LettuceCacheManager
            ?: error("CacheManager가 LettuceCacheManager가 아닙니다.")

        return manager.getCache(cacheName)
            ?: manager.createCache(
                cacheName,
                lettuceCacheConfigOf<K, V>(
                    ttlSeconds = ttlSeconds,
                    serializer = serializer,
                )
            )
    }

    /**
     * 커스텀 [Configuration]을 이용해 [JCache]`<K, V>`를 생성하거나 가져옵니다.
     */
    inline fun <reified K: Any, reified V: Any> getOrCreate(
        cacheName: String,
        configuration: Configuration<K, V>,
    ): JCache<K, V> {
        val manager = cacheManager as? LettuceCacheManager
            ?: error("CacheManager가 LettuceCacheManager가 아닙니다.")

        return manager.getCache(cacheName)
            ?: manager.createCache(cacheName, configuration)
    }
}
