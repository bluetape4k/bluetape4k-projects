package io.bluetape4k.cache.nearcache.hazelcast

import com.github.benmanes.caffeine.cache.Caffeine
import com.hazelcast.client.cache.HazelcastClientCachingProvider
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
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

/**
 * Hazelcast back cache 기반 [NearSuspendCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - 기본 front suspend cache는 Caffeine 구현을 사용합니다.
 * - back suspend cache는 `HazelcastSuspendCache`를 생성해 연결합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = HazelcastNearSuspendCache<String, Int>("scores")
 * // near.isClosed() == false
 * ```
 */
object HazelcastNearSuspendCache {

    /**
     * front/back suspend cache를 직접 지정해 [NearSuspendCache]를 생성합니다.
     *
     * ## 동작/계약
     * - [frontSuspendCache], [backSuspendCache] 인스턴스를 그대로 사용합니다.
     * - [checkExpiryPeriod]는 back 캐시 만료 검사 주기로 전달됩니다.
     * - 새 near suspend cache 래퍼를 반환합니다.
     *
     * ```kotlin
     * val near = HazelcastNearSuspendCache(front, back)
     * // near.containsKey("k1") == false
     * ```
     */
    operator fun <K: Any, V: Any> invoke(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): NearSuspendCache<K, V> = NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * back cache 이름으로 back cache를 만들고 기본 Caffeine front cache와 결합합니다.
     *
     * ## 동작/계약
     * - [backCacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
     * - [frontCacheBuilder]로 front Caffeine 설정을 커스터마이즈할 수 있습니다.
     * - 반환된 near cache는 생성 시점에 front/back 캐시를 각각 새로 래핑합니다.
     *
     * ```kotlin
     * val near = HazelcastNearSuspendCache<String, String>("users") { maximumSize(1_000) }
     * // near.isClosed() == false
     * ```
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
        val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }

    /**
     * back cache 이름으로 back cache를 만들고 지정된 front cache와 결합합니다.
     *
     * ## 동작/계약
     * - [backCacheName] blank면 `IllegalArgumentException`이 발생합니다.
     * - [frontSuspendCache]는 호출자가 관리하는 인스턴스를 그대로 사용합니다.
     * - [checkExpiryPeriod] 값은 near cache 내부 만료 검사 스케줄에 반영됩니다.
     *
     * ```kotlin
     * val near = HazelcastNearSuspendCache("users", front)
     * // near.get("u:1") == null
     * ```
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
        val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }
}
