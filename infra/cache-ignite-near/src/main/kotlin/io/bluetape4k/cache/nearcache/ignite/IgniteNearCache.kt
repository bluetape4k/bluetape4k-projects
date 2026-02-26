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
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import kotlin.BuilderInference

/**
 * Apache Ignite 2 기반 Back Cache를 사용하는 [NearCache] 생성 유틸리티입니다.
 *
 * 기본 Front Cache는 Caffeine이며, [NearCacheConfig]로 Front 구성을 변경할 수 있습니다.
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
 * Apache Ignite 2 기반 Back Cache를 사용하는 [NearSuspendCache] 생성 유틸리티입니다.
 *
 * 기본 Front SuspendCache는 Caffeine이며, 사용자 정의 Front SuspendCache를 지정할 수 있습니다.
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
