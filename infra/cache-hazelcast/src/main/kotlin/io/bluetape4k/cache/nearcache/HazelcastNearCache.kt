package io.bluetape4k.cache.nearcache

import com.hazelcast.client.cache.HazelcastClientCachingProvider
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.support.requireNotBlank
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast back cache를 사용하는 [NearCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - front cache는 [NearCacheConfig] 설정을 사용하며 기본 구현은 Caffeine입니다.
 * - back cache는 전달된 JCache를 사용하거나 cache name으로 조회/생성합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = HazelcastNearCache<String, String>("users")
 * // near.getName().isNotBlank() == true
 * ```
 */
object HazelcastNearCache {

    /**
     * 기존 back cache 인스턴스로 [NearCache]를 생성합니다.
     *
     * ## 동작/계약
     * - [backCache]를 그대로 back 계층으로 사용합니다.
     * - [nearCacheConfig]의 front 설정과 만료 검사 주기를 적용합니다.
     * - 새 near-cache 래퍼를 생성해 반환합니다.
     *
     * ```kotlin
     * val near = HazelcastNearCache(backCache)
     * // near.unwrap(javax.cache.Cache::class.java) != null
     * ```
     */
    operator fun <K: Any, V: Any> invoke(
        backCache: JCache<K, V>,
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = NearCache(nearCacheConfig, backCache)

    /**
     * cache name으로 back cache를 조회/생성한 뒤 [NearCache]를 생성합니다.
     *
     * ## 동작/계약
     * - [backCacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
     * - 동일 이름 back cache가 있으면 재사용, 없으면 [backCacheConfiguration]으로 생성합니다.
     * - 생성된 back cache와 [nearCacheConfig]를 결합해 near cache를 반환합니다.
     *
     * ```kotlin
     * val near = HazelcastNearCache<String, Int>("scores")
     * // near.getName() == "scores"
     * ```
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val manager = HazelcastClientCachingProvider().cacheManager
        val backCache = manager.getCache(backCacheName, K::class.java, V::class.java)
            ?: manager.createCache(backCacheName, backCacheConfiguration)
        return NearCache(nearCacheConfig, backCache)
    }
}
