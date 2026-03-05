package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.support.requireNotBlank
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider

/**
 * Ignite back cache를 사용하는 [NearCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - front cache는 [NearCacheConfig] 설정을 사용하며 기본 구현은 Caffeine입니다.
 * - back cache는 전달된 JCache를 사용하거나 cache name으로 조회/생성합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = IgniteNearCache<String, String>("users")
 * // near.getName() == "users"
 * ```
 */
object IgniteNearCache {

    /**
     * 기존 Back Cache 인스턴스로 [NearCache]를 생성합니다.
     *
     * @param backCache Ignite 기반 Back Cache
     * @param nearCacheConfig Near Cache 설정
     */
    operator fun <K: Any, V: Any> invoke(
        backCache: JCache<K, V>,
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = NearCache(nearCacheConfig, backCache)

    /**
     * Ignite CacheManager에서 Back Cache를 조회/생성한 뒤 [NearCache]를 생성합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param backCacheConfiguration Back Cache 설정
     * @param nearCacheConfig Near Cache 설정
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val manager = IgniteCachingProvider().cacheManager
        val backCache = manager.getCache(backCacheName, K::class.java, V::class.java)
            ?: manager.createCache(backCacheName, backCacheConfiguration)
        return NearCache(nearCacheConfig, backCache)
    }
}
