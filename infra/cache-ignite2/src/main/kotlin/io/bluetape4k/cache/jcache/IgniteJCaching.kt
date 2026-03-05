package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider

/**
 * [Apache Ignite 2.x](https://ignite.apache.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object 입니다.
 */
object IgniteJCaching: KLogging() {

    /**
     * Ignite 2.x 에서 제공하는 [CacheManager]
     */
    val cacheManager: CacheManager by lazy { IgniteCachingProvider().cacheManager }

    /**
     * [CacheManager]에서 [JCache]`<K, V>`를 생성하거나 가져옵니다.
     *
     * ```
     * val cache = JIgnite2Caching.getOrCreate<String, Any>("my-cache")
     * ```
     *
     * @param name 캐시 이름
     * @param configuration 캐시 설정
     */
    inline fun <reified K, reified V> getOrCreate(
        name: String,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
    ): JCache<K, V> =
        cacheManager.getOrCreate(name, configuration)
}
