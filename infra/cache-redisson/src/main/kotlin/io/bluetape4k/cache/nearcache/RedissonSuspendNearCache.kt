package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson back cache 기반 [SuspendNearCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - 기본 front suspend cache는 Caffeine 구현을 사용합니다.
 * - back suspend cache는 `RedissonSuspendCache`를 생성해 연결합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = RedissonNearSuspendCache<String, Int>("scores", redisson)
 * // near.isClosed() == false
 * ```
 */
object RedissonSuspendNearCache {

    /**
     * 기본 Codec: [RedissonCodecs.LZ4Fory]
     */
    val defaultNearCacheCodec = RedissonCodecs.LZ4Fory


    /**
     * Front/Back SuspendCache를 직접 지정해 [SuspendNearCache]를 생성합니다.
     *
     * @param frontSuspendCache Front SuspendCache
     * @param backSuspendCache Back SuspendCache
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     */
    operator fun <K: Any, V: Any> invoke(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> = SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * Back Cache 이름으로 Back SuspendCache를 생성하고 [SuspendNearCache]를 생성합니다.
     *
     * Front SuspendCache는 기본적으로 Caffeine을 사용합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param redisson Redisson 클라이언트
     * @param backCacheConfiguration Back Cache 설정
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     * @param frontCacheBuilder Front Caffeine 설정 빌더
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        redisson: RedissonClient,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        @BuilderInference noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): SuspendNearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val frontSuspendCache = CaffeineSuspendCache<K, V>(frontCacheBuilder)
        val backSuspendCache = RedissonSuspendCache<K, V>(backCacheName, redisson, backCacheConfiguration)
        return SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }

    /**
     * Back Cache 이름으로 Back SuspendCache를 생성하고, 지정된 Front SuspendCache와 조합합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param redisson Redisson 클라이언트
     * @param frontSuspendCache Front SuspendCache
     * @param backCacheConfiguration Back Cache 설정
     * @param checkExpiryPeriod Back Cache 만료 검사 주기(ms)
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        redisson: RedissonClient,
        frontSuspendCache: SuspendCache<K, V>,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")
        val backSuspendCache = RedissonSuspendCache<K, V>(backCacheName, redisson, backCacheConfiguration)
        return SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)
    }
}
