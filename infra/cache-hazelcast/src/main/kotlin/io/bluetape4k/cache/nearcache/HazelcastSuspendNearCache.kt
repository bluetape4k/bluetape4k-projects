package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.support.requireNotBlank
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast back cache 기반 [SuspendNearCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - 기본 front suspend cache는 Caffeine 구현을 사용합니다.
 * - back suspend cache는 `HazelcastSuspendCache`를 생성해 연결합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = HazelcastSuspendNearCache<String, Int>("scores")
 * // near.isClosed() == false
 * ```
 */
object HazelcastSuspendNearCache {

    /**
     * front/back suspend cache를 직접 지정해 [SuspendNearCache]를 생성합니다.
     *
     * ## 동작/계약
     * - [frontSuspendCache], [backSuspendCache] 인스턴스를 그대로 사용합니다.
     * - [checkExpiryPeriod]는 back 캐시 만료 검사 주기로 전달됩니다.
     * - 새 near suspend cache 래퍼를 반환합니다.
     *
     * ```kotlin
     * val near = HazelcastSuspendNearCache(front, back)
     * // near.containsKey("k1") == false
     * ```
     */
    operator fun <K: Any, V: Any> invoke(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> = SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * back cache 이름으로 back cache를 만들고 기본 Caffeine front cache와 결합합니다.
     *
     * ## 동작/계약
     * - [backCacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
     * - [frontCacheBuilder]로 front Caffeine 설정을 커스터마이즈할 수 있습니다.
     * - 반환된 near cache는 생성 시점에 front/back 캐시를 각각 새로 래핑합니다.
     *
     * ```kotlin
     * val near = HazelcastSuspendNearCache<String, String>("users") { maximumSize(1_000) }
     * // near.isClosed() == false
     * ```
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): SuspendNearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
        val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
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
     * val near = HazelcastSuspendNearCache("users", front)
     * // near.get("u:1") == null
     * ```
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        frontSuspendCache: SuspendCache<K, V>,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")
        val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
        return SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }
}
