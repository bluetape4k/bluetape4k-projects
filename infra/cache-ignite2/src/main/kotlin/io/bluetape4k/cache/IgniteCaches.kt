package io.bluetape4k.cache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache
import io.bluetape4k.cache.jcache.IgniteJCaching
import io.bluetape4k.cache.jcache.IgniteSuspendCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.nearcache.IgniteNearCache
import io.bluetape4k.cache.nearcache.IgniteSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.SuspendNearCache
import io.bluetape4k.logging.KLogging
import org.apache.ignite.client.ClientCache
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import kotlin.experimental.ExperimentalTypeInference

/**
 * Ignite 2.x 캐시 인스턴스 생성을 위한 진입점 object 입니다.
 *
 * JCache, SuspendCache, NearCache, SuspendNearCache 팩토리 함수를 제공합니다.
 */
object IgniteCaches: KLogging() {

    /**
     * Ignite CacheManager에서 [JCache]`<K, V>`를 조회하거나 생성합니다.
     *
     * @param name 캐시 이름
     * @param configuration 캐시 설정
     */
    inline fun <reified K, reified V> jcache(
        name: String,
        configuration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
    ): JCache<K, V> = IgniteJCaching.getOrCreate(name, configuration)

    /**
     * 기존 [JCache] 인스턴스를 [IgniteSuspendCache]로 감쌉니다.
     *
     * @param jcache 래핑할 JCache 인스턴스
     */
    fun <K: Any, V: Any> suspendCache(jcache: JCache<K, V>): IgniteSuspendCache<K, V> =
        IgniteSuspendCache(jcache)

    /**
     * 캐시 이름과 설정으로 [IgniteSuspendCache]를 생성합니다.
     *
     * @param cacheName 캐시 이름
     * @param configuration 캐시 설정
     */
    fun <K: Any, V: Any> suspendCache(
        cacheName: String,
        configuration: Configuration<K, V> = MutableConfiguration(),
    ): IgniteSuspendCache<K, V> = IgniteSuspendCache(cacheName, configuration)

    /**
     * Ignite [ClientCache] 인스턴스를 [IgniteClientSuspendCache]로 감쌉니다.
     *
     * @param cache 래핑할 ClientCache 인스턴스
     */
    fun <K: Any, V: Any> clientSuspendCache(cache: ClientCache<K, V>): IgniteClientSuspendCache<K, V> =
        IgniteClientSuspendCache(cache)

    /**
     * 기존 Back [JCache]와 [NearCacheConfig]로 [NearCache]를 생성합니다.
     *
     * @param backCache Ignite 기반 Back Cache
     * @param nearCacheConfig Near Cache 설정
     */
    fun <K: Any, V: Any> nearCache(
        backCache: JCache<K, V>,
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = IgniteNearCache(backCache, nearCacheConfig)

    /**
     * Back Cache 이름으로 Back Cache를 조회/생성한 뒤 [NearCache]를 생성합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param backCacheConfiguration Back Cache 설정
     * @param nearCacheConfig Near Cache 설정
     */
    inline fun <reified K: Any, reified V: Any> nearCache(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = IgniteNearCache(backCacheName, backCacheConfiguration, nearCacheConfig)

    /**
     * Front/Back [SuspendCache]를 직접 지정해 [SuspendNearCache]를 생성합니다.
     *
     * @param frontSuspendCache Front SuspendCache
     * @param backSuspendCache Back SuspendCache
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     */
    fun <K: Any, V: Any> suspendNearCache(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> = IgniteSuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * Back Cache 이름으로 Back SuspendCache를 생성하고, Caffeine front와 조합한 [SuspendNearCache]를 생성합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param backCacheConfiguration Back Cache 설정
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     * @param frontCacheBuilder Front Caffeine 설정 빌더
     */
    @OptIn(ExperimentalTypeInference::class)
    inline fun <reified K: Any, reified V: Any> suspendNearCache(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): SuspendNearCache<K, V> =
        IgniteSuspendNearCache(backCacheName, backCacheConfiguration, checkExpiryPeriod, frontCacheBuilder)
}
