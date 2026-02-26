package io.bluetape4k.cache.jcache

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import com.hazelcast.client.cache.HazelcastClientCachingProvider
import org.ehcache.jsr107.EhcacheCachingProvider
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider
import org.redisson.api.RedissonClient
import org.redisson.jcache.JCachingProvider
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * [javax.cache.Cache]`<K, V>` 를 제공하는 object 입니다.
 */
object JCaching {

    /**
     * [Cache2k](https://cache2k.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Cache2k {
        /**
         * Cache2K 에서 제공하는 [javax.cache.CacheManager]
         */
        val cacheManager: CacheManager by lazy { jcacheManager<org.cache2k.jcache.provider.JCacheProvider>() }

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
         * Caffeine 에서 제공하는 [javax.cache.CacheManager]
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
         * Ehcache 에서 제공하는 [javax.cache.CacheManager]
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

    /**
     * [Redisson](https://redisson.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Redisson {

        /**
         * Redisson 에서 제공하는 [javax.cache.CacheManager]
         */
        val cacheManager by lazy { jcacheManager<JCachingProvider>() }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         *
         * ```
         * val redisson: RedissonClient = ...
         * val cache = JCaching.Redisson.getOrCreate<String, String>("default", redisson) {
         *    this.withExpiryPolicy(TouchedExpiryPolicy(Duration.ONE_MINUTE))
         *    this.withValueSerializer(StringSerializer())
         *    this.withKeySerializer(StringSerializer())
         *    this.withStatisticsEnabled()
         *    this.withManagementEnabled()
         * }
         * ```
         *
         * @param cacheName 캐시 이름
         * @param redisson Redisson 클라이언트
         * @param configuration 캐시 설정
         */
        inline fun <reified K, reified V> getOrCreate(
            cacheName: String,
            redisson: RedissonClient,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> {
            val redissonConfiguration = RedissonConfiguration.fromInstance(redisson, configuration)
            return cacheManager.getOrCreate(cacheName, redissonConfiguration)
        }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         *
         * ```
         * val redissonConfig: org.redisson.config.Config = ...
         * val cache = JCaching.Redisson.getOrCreate<String, String>("default", redissonConfig) {
         *     this.withExpiryPolicy(TouchedExpiryPolicy(Duration.ONE_MINUTE))
         *     this.withValueSerializer(StringSerializer())
         *     this.withKeySerializer(StringSerializer())
         *     this.withStatisticsEnabled()
         *     this.withManagementEnabled()
         * }
         * ```
         *
         * @param cacheName 캐시 이름
         * @param redissonConfig Redisson 설정
         * @param configuration 캐시 설정
         * @return [JCache]`<K, V>` 인스턴스
         */
        inline fun <reified K, reified V> getOrCreate(
            cacheName: String,
            redissonConfig: org.redisson.config.Config,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> {
            val redissonConfiguration = RedissonConfiguration.fromConfig(redissonConfig, configuration)
            return cacheManager.getOrCreate(cacheName, redissonConfiguration)
        }

        /**
         * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
         *
         * ```
         * val cache = JCaching.Redisson.getOrCreateCache<String, String>("default", redisson) {
         *     this.withExpiryPolicy(TouchedExpiryPolicy(Duration.ONE_MINUTE))
         *     this.withValueSerializer(StringSerializer())
         *     this.withKeySerializer(StringSerializer())
         *     this.withStatisticsEnabled()
         * }
         * ```
         *
         * @param cacheName 캐시 이름
         * @param redisson Redisson 클라이언트
         * @param configuration 캐시 설정
         */
        fun <K, V> getOrCreateCache(
            cacheName: String,
            redisson: RedissonClient,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): JCache<K, V> {
            return with(jcacheManager<JCachingProvider>()) {
                getCache(cacheName)
                    ?: run {
                        val redissonCfg = RedissonConfiguration.fromInstance(redisson, configuration)
                        createCache(cacheName, redissonCfg)
                    }
            }
        }
    }

    /**
     * [Hazelcast](https://hazelcast.com/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Hazelcast {
        /**
         * Hazelcast 에서 제공하는 [javax.cache.CacheManager]
         */
        val cacheManager by lazy { HazelcastClientCachingProvider().cacheManager }

        /**
         * [CacheManager]에서 [JCache]`<K, V>`를 생성하거나 가져옵니다.
         */
        inline fun <reified K, reified V> getOrCreate(
            name: String,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> =
            cacheManager.getOrCreate(name, configuration)
    }

    /**
     * [Apache Ignite 2.x](https://ignite.apache.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공합니다.
     */
    object Ignite2 {
        /**
         * Ignite 2.x 에서 제공하는 [javax.cache.CacheManager]
         */
        val cacheManager by lazy { IgniteCachingProvider().cacheManager }

        /**
         * [CacheManager]에서 [JCache]`<K, V>`를 생성하거나 가져옵니다.
         */
        inline fun <reified K, reified V> getOrCreate(
            name: String,
            configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
        ): JCache<K, V> =
            cacheManager.getOrCreate(name, configuration)
    }
}
