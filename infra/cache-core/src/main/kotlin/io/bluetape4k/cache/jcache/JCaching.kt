package io.bluetape4k.cache.jcache

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import io.bluetape4k.logging.KLogging
import org.cache2k.jcache.provider.JCacheProvider
import org.ehcache.jsr107.EhcacheCachingProvider
import javax.cache.CacheManager
import javax.cache.configuration.Configuration

/**
 * [javax.cache.Cache]`<K, V>` 를 제공하는 object 입니다.
 */
object JCaching: KLogging() {

    /**
     * [Cache2k](https://cache2k.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Cache2k {
        /**
         * Cache2K 에서 제공하는 [CacheManager]
         */
        val cacheManager: CacheManager by lazy { jcacheManager<JCacheProvider>() }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         */
        inline fun <reified K, reified V> getOrCreate(
            name: String,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> =
            cacheManager.getOrCreate(name, configuration)
    }

    /**
     * [Caffeine](https://github.com/ben-manes/caffeine) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Caffeine {
        /**
         * Caffeine 에서 제공하는 [CacheManager]
         */
        val cacheManager by lazy { jcacheManager<CaffeineCachingProvider>() }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         *
         * ```
         * val cache = JCaching.Caffeine.getOrCreate<String, String>("default") {
         *    this.withExpiryPolicy(TouchedExpiryPolicy(Duration.ONE_MINUTE))
         *    this.withValueSerializer(StringSerializer())
         *    this.withKeySerializer(StringSerializer())
         * }
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

    /**
     * [Ehcache](https://www.ehcache.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object EhCache {

        /**
         * Ehcache 에서 제공하는 [CacheManager]
         */
        val cacheManager by lazy { jcacheManager<EhcacheCachingProvider>() }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         */
        inline fun <reified K, reified V> getOrCreate(
            name: String,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> =
            cacheManager.getOrCreate(name, configuration)
    }

}
