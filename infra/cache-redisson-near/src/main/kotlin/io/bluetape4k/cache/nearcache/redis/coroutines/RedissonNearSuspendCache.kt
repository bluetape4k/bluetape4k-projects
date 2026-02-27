package io.bluetape4k.cache.nearcache.redis.coroutines

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.coroutines.RedissonSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson 기반 Back Cache를 사용하는 전용 [NearSuspendCache] 래퍼입니다.
 *
 * 기본 Front SuspendCache는 Caffeine을 사용하며, 사용자 정의 Front SuspendCache를 지정할 수 있습니다.
 */
class RedissonNearSuspendCache<K: Any, V: Any> private constructor(
    private val nearSuspendCache: NearSuspendCache<K, V>,
): SuspendCache<K, V> by nearSuspendCache {

    companion object {
        /**
         * Front/Back SuspendCache를 직접 지정해 [RedissonNearSuspendCache]를 생성합니다.
         */
        operator fun <K: Any, V: Any> invoke(
            frontSuspendCache: SuspendCache<K, V>,
            backSuspendCache: SuspendCache<K, V>,
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): RedissonNearSuspendCache<K, V> =
            RedissonNearSuspendCache(NearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod))

        /**
         * Back Cache 이름으로 Back SuspendCache를 생성하고 [RedissonNearSuspendCache]를 생성합니다.
         *
         * Front SuspendCache는 기본적으로 Caffeine을 사용합니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            redisson: RedissonClient,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
            @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
        ): RedissonNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")

            val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
            val backSuspendCache = RedissonSuspendCache<K, V>(backCacheName, redisson, backCacheConfiguration)
            return RedissonNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
        }

        /**
         * Back Cache 이름으로 Back SuspendCache를 생성하고, 지정된 Front SuspendCache와 조합합니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            backCacheName: String,
            redisson: RedissonClient,
            frontSuspendCache: SuspendCache<K, V>,
            backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
            checkExpiryPeriod: Long = NearSuspendCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        ): RedissonNearSuspendCache<K, V> {
            backCacheName.requireNotBlank("backCacheName")
            val backSuspendCache = RedissonSuspendCache<K, V>(backCacheName, redisson, backCacheConfiguration)
            return RedissonNearSuspendCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
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
