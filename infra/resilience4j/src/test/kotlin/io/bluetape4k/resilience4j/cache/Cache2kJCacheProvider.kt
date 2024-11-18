package io.bluetape4k.resilience4j.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import org.cache2k.Cache2kBuilder
import org.cache2k.jcache.ExtendedMutableConfiguration
import org.cache2k.jcache.provider.JCacheProvider
import java.util.concurrent.TimeUnit
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.spi.CachingProvider

object Cache2kJCacheProvider: KLogging() {

    private val cachingProvider: CachingProvider by lazy { Caching.getCachingProvider(JCacheProvider::class.qualifiedName) }
    private val cacheManager: CacheManager by lazy { cachingProvider.cacheManager }

    /**
     * Cache2k 의 [JCache] 를 빌드합니다.
     *
     * @param name cache name
     */
    fun <K, V> getCache(
        name: String,
        keyClass: Class<K>,
        valueClass: Class<V>,
    ): Cache<K, V> {
        name.requireNotBlank("name")

        return if (cacheManager.cacheNames.contains(name)) {
            log.info { "Get exist cache. name=$name" }
            cacheManager.getCache(name, keyClass, valueClass)
        } else {
            log.info { "Create cache. name=$name" }
            val cacheBuilder = Cache2kBuilder
                .of(keyClass, valueClass)
                .entryCapacity(10_000)
                .expireAfterWrite(5, TimeUnit.HOURS)
            val cacheConfig = ExtendedMutableConfiguration.of(cacheBuilder)

            cacheManager.createCache(name, cacheConfig)
        }
    }

    /**
     * Cache2k 의 JCache 를 빌드합니다.
     *
     * @param name cache name
     */
    inline fun <reified K, reified V> getJCache(name: String): Cache<K, V> {
        name.requireNotBlank("name")
        return getCache(name, K::class.java, V::class.java)
    }
}
