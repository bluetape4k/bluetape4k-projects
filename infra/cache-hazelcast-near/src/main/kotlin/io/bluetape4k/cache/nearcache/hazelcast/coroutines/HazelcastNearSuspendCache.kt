package io.bluetape4k.cache.nearcache.hazelcast.coroutines

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
import io.bluetape4k.support.requireNotBlank
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast back cache를 사용하는 [NearSuspendCache] 래퍼입니다.
 *
 * ## 동작/계약
 * - 내부적으로 [NearSuspendCache] 인스턴스에 연산을 위임합니다.
 * - 기본 front cache는 Caffeine 기반 [CaffeineSuspendCache]를 사용합니다.
 * - `backCacheName`이 blank면 팩토리 함수에서 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val cache = HazelcastNearSuspendCache<String, Int>("scores")
 * // cache.isClosed() == false
 * ```
 */
class HazelcastNearSuspendCache<K: Any, V: Any> private constructor(
    private val nearSuspendCache: NearSuspendCache<K, V>,
): SuspendCache<K, V> by nearSuspendCache {

    companion object {
        /**
         * front/back suspend cache를 직접 지정해 [HazelcastNearSuspendCache]를 생성합니다.
         *
         * ## 동작/계약
         * - 전달된 [frontSuspendCache], [backSuspendCache] 인스턴스를 그대로 사용합니다.
         * - [checkExpiryPeriod]를 near cache 만료 검사 주기로 전달합니다.
         *
         * ```kotlin
         * val cache = HazelcastNearSuspendCache(front, back)
         * // cache.asNearSuspendCache().checkExpiryPeriod == NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD
         * ```
         */
        operator fun <K: Any, V: Any> invoke(
            frontSuspendCache: SuspendCache<K, V>,
            backSuspendCache: SuspendCache<K, V>,
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): HazelcastNearSuspendCache<K, V> =
            HazelcastNearSuspendCache(NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod))

        /**
         * back cache 이름으로 back cache를 만들고 기본 Caffeine front cache와 결합합니다.
         *
         * ## 동작/계약
         * - [backCacheName] blank면 `IllegalArgumentException`이 발생합니다.
         * - [frontCacheBuilder]로 front cache 용량/만료 정책을 조정할 수 있습니다.
         * - 반환 객체는 새 near suspend cache 래퍼입니다.
         *
         * ```kotlin
         * val cache = HazelcastNearSuspendCache<String, String>("users") { maximumSize(100) }
         * // cache.isClosed() == false
         * ```
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
            @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
        ): HazelcastNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")

            val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
            val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
            return HazelcastNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
        }

        /**
         * back cache 이름으로 back cache를 만들고 지정된 front cache와 결합합니다.
         *
         * ## 동작/계약
         * - [backCacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
         * - [frontSuspendCache]는 호출자가 관리하는 인스턴스를 그대로 사용합니다.
         * - [checkExpiryPeriod]는 near cache 내부 만료 검사 주기로 적용됩니다.
         *
         * ```kotlin
         * val cache = HazelcastNearSuspendCache("users", front)
         * // cache.asNearSuspendCache() != null
         * ```
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            frontSuspendCache: SuspendCache<K, V>,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): HazelcastNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")
            val backSuspendCache = HazelcastSuspendCache<K, V>(backCacheName, backCacheConfiguration)
            return HazelcastNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
        }
    }

    /**
     * front/back 캐시를 모두 비웁니다.
     *
     * ## 동작/계약
     * - 내부 [NearSuspendCache.clearAll]을 호출합니다.
     * - front와 back 모두에 대해 삭제를 시도합니다.
     *
     * ```kotlin
     * cache.clearAll()
     * // cache.get("k1") == null
     * ```
     */
    suspend fun clearAll() = nearSuspendCache.clearAll()

    /**
     * 내부 [NearSuspendCache] 인스턴스를 반환합니다.
     *
     * ## 동작/계약
     * - 래핑된 동일 인스턴스 참조를 그대로 반환합니다.
     * - 추가 객체를 생성하지 않습니다.
     *
     * ```kotlin
     * val raw = cache.asNearSuspendCache()
     * // raw === cache.asNearSuspendCache()
     * ```
     */
    fun asNearSuspendCache(): NearSuspendCache<K, V> = nearSuspendCache
}
