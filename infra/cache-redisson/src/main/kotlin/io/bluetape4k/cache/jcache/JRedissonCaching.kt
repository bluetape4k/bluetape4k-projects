package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.JCachingProvider
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * [Redisson](https://redisson.org/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object 입니다.
 */
object JRedissonCaching: KLogging() {

    /**
     * Redisson 에서 제공하는 [CacheManager]
     */
    val cacheManager: CacheManager by lazy { jcacheManager<JCachingProvider>() }

    /**
     * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
     *
     * ```
     * val redisson: RedissonClient = ...
     * val cache = JRedissonCaching.getOrCreate<String, String>("default", redisson)
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
     * val cache = JRedissonCaching.getOrCreate<String, String>("default", redissonConfig)
     * ```
     *
     * @param cacheName 캐시 이름
     * @param redissonConfig Redisson 설정
     * @param configuration 캐시 설정
     */
    inline fun <reified K, reified V> getOrCreate(
        cacheName: String,
        redissonConfig: Config,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
    ): JCache<K, V> {
        val redissonConfiguration = RedissonConfiguration.fromConfig(redissonConfig, configuration)
        return cacheManager.getOrCreate(cacheName, redissonConfiguration)
    }

    /**
     * [CacheManager] 에서 [JCache]`<K, V>` 를 생성하거나 가져옵니다.
     *
     * ```
     * val cache = JRedissonCaching.getOrCreateCache<String, String>("default", redisson)
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
