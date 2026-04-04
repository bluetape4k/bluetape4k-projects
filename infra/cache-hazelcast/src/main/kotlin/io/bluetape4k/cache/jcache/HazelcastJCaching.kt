package io.bluetape4k.cache.jcache

import com.hazelcast.cache.HazelcastCachingProvider
import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.logging.KLogging
import javax.cache.CacheManager
import javax.cache.configuration.Configuration

/**
 * [Hazelcast](https://hazelcast.com/) 를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object 입니다.
 *
 * ```kotlin
 * val cache = HazelcastJCaching.getOrCreate<String, String>(hazelcastInstance, "my-cache")
 * cache.put("key", "value")
 * val value = cache.get("key")
 * // value == "value"
 * ```
 */
object HazelcastJCaching: KLogging() {

    /**
     * 주어진 [HazelcastInstance]를 이용해 [CacheManager]를 생성합니다.
     *
     * `HazelcastClientCachingProvider` 대신 `HazelcastCachingProvider.propertiesByInstanceItself`를 사용하여
     * 이미 연결된 인스턴스를 재사용합니다.
     *
     * ```kotlin
     * val manager = HazelcastJCaching.cacheManagerOf(hazelcastInstance)
     * // manager != null
     * ```
     *
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @return 연결된 [CacheManager]
     */
    fun cacheManagerOf(hazelcastInstance: HazelcastInstance): CacheManager {
        val provider = HazelcastCachingProvider()
        val properties = HazelcastCachingProvider.propertiesByInstanceItself(hazelcastInstance)
        return provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)
    }

    /**
     * [CacheManager]에서 [JCache]`<K, V>`를 생성하거나 가져옵니다.
     *
     * ```kotlin
     * val cache = HazelcastJCaching.getOrCreate<String, String>(hazelcastInstance, "my-cache")
     * cache.put("greeting", "hello")
     * val result = cache.get("greeting")
     * // result == "hello"
     * ```
     *
     * @param hazelcastInstance 연결된 Hazelcast 인스턴스
     * @param name 캐시 이름
     * @param configuration 캐시 설정
     * @return [JCache] 인스턴스
     */
    inline fun <reified K, reified V> getOrCreate(
        hazelcastInstance: HazelcastInstance,
        name: String,
        configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
    ): JCache<K, V> =
        cacheManagerOf(hazelcastInstance).getOrCreate(name, configuration)
}
