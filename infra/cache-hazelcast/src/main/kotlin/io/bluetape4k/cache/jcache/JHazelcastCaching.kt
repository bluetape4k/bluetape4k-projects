package io.bluetape4k.cache.jcache

import com.hazelcast.client.cache.HazelcastClientCachingProvider
import io.bluetape4k.logging.KLogging
import javax.cache.CacheManager
import javax.cache.configuration.Configuration

/**
 * [Hazelcast](https://hazelcast.com/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object 입니다.
 */
object JHazelcastCaching: KLogging() {

    /**
     * Hazelcast 에서 제공하는 [CacheManager]
     */
    val cacheManager: CacheManager by lazy { HazelcastClientCachingProvider().cacheManager }

    /**
     * [CacheManager]에서 [JCache]`<K, V>`를 생성하거나 가져옵니다.
     *
     * ```
     * val cache = JHazelcastCaching.getOrCreate<String, Any>("my-cache")
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
