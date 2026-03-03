package io.bluetape4k.cache.nearcache.ignite

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.Ignite2SuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
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

/**
 * Ignite back cache 기반 [NearSuspendCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - 기본 front suspend cache는 Caffeine 구현을 사용합니다.
 * - back suspend cache는 `Ignite2SuspendCache`를 생성해 연결합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = IgniteNearSuspendCache<String, Int>("scores")
 * // near.isClosed() == false
 * ```
 */
object IgniteNearSuspendCache {

    /**
     * Front/Back SuspendCache를 직접 지정해 [NearSuspendCache]를 생성합니다.
     *
     * @param frontSuspendCache Front SuspendCache
     * @param backSuspendCache Back SuspendCache
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     */
    operator fun <K: Any, V: Any> invoke(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): NearSuspendCache<K, V> = NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * Back Cache 이름으로 Back SuspendCache를 생성하고 [NearSuspendCache]를 생성합니다.
     *
     * Front SuspendCache는 기본적으로 Caffeine을 사용합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param backCacheConfiguration Back Cache 설정
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     * @param frontCacheBuilder Front Caffeine 설정 빌더
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): NearSuspendCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
        val backSuspendCache = Ignite2SuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }

    /**
     * Back Cache 이름으로 Back SuspendCache를 생성하고, 지정된 Front SuspendCache와 조합합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param frontSuspendCache Front SuspendCache
     * @param backCacheConfiguration Back Cache 설정
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        frontSuspendCache: SuspendCache<K, V>,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): NearSuspendCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")
        val backSuspendCache = Ignite2SuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }
}
