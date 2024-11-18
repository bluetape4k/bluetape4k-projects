package io.bluetape4k.resilience4j.cache

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.assertNotBlank
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.spi.CachingProvider
import kotlin.reflect.KClass

object CaffeineJCacheProvider: KLogging() {

    private val cachingProvider: CachingProvider by lazy {
        Caching.getCachingProvider(CaffeineCachingProvider::class.qualifiedName)
    }
    private val cacheManager: CacheManager by lazy {
        cachingProvider.cacheManager
    }


    fun <K: Any, V: Any> getCache(
        name: String,
        keyClass: KClass<K>,
        valueClass: KClass<V>,
    ): Cache<K, V> {
        name.assertNotBlank("name")

        return if (cacheManager.cacheNames.contains(name)) {
            log.info { "Get exist cache. name=$name" }
            cacheManager.getCache(name, keyClass.java, valueClass.java)
        } else {
            log.info { "Create cache. name=$name" }
            val cacheConfig = CaffeineConfiguration<K, V>()
            cacheManager.createCache(name, cacheConfig)
        }
    }

    inline fun <reified K: Any, reified V: Any> getJCache(name: String): Cache<K, V> =
        getCache(name, K::class, V::class)
}
