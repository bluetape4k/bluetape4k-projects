package io.bluetape4k.cache.nearcache.ignite.coroutines

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.Ignite2SuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
import io.bluetape4k.support.requireNotBlank
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Ignite back cache를 사용하는 [NearSuspendCache] 래퍼입니다.
 *
 * ## 동작/계약
 * - 내부적으로 [NearSuspendCache] 인스턴스에 연산을 위임합니다.
 * - 기본 front cache는 Caffeine 기반 [CaffeineSuspendCache]를 사용합니다.
 * - `backCacheName`이 blank면 팩토리 함수에서 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val cache = IgniteNearSuspendCache<String, Int>("scores")
 * // cache.isClosed() == false
 * ```
 */
class IgniteNearSuspendCache<K: Any, V: Any> private constructor(
    private val nearSuspendCache: NearSuspendCache<K, V>,
): SuspendCache<K, V> by nearSuspendCache {

    companion object {
        /**
         * Front/Back SuspendCache를 직접 지정해 [IgniteNearSuspendCache]를 생성합니다.
         */
        operator fun <K: Any, V: Any> invoke(
            frontSuspendCache: SuspendCache<K, V>,
            backSuspendCache: SuspendCache<K, V>,
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): IgniteNearSuspendCache<K, V> =
            IgniteNearSuspendCache(NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod))

        /**
         * Back Cache 이름으로 Back SuspendCache를 생성하고 [IgniteNearSuspendCache]를 생성합니다.
         *
         * Front SuspendCache는 기본적으로 Caffeine을 사용합니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
            @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
        ): IgniteNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")

            val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
            val backSuspendCache = Ignite2SuspendCache<K, V>(backCacheName, backCacheConfiguration)
            return IgniteNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
        }

        /**
         * Back Cache 이름으로 Back SuspendCache를 생성하고, 지정된 Front SuspendCache와 조합합니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            frontSuspendCache: SuspendCache<K, V>,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): IgniteNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")
            val backSuspendCache = Ignite2SuspendCache<K, V>(backCacheName, backCacheConfiguration)
            return IgniteNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
        }
    }

    /**
     * Front/Back cache를 모두 비웁니다.
     */
    suspend fun clearAll() = nearSuspendCache.clearAll()

    /**
     * 내부 [NearSuspendCache] 인스턴스를 반환합니다.
     */
    fun asNearSuspendCache(): NearSuspendCache<K, V> = nearSuspendCache
}
