package io.bluetape4k.cache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonJCaching
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.RedissonNearCache
import io.bluetape4k.cache.nearcache.RedissonResp3NearCache
import io.bluetape4k.cache.nearcache.RedissonResp3NearCacheConfig
import io.bluetape4k.cache.nearcache.RedissonResp3SuspendNearCache
import io.bluetape4k.cache.nearcache.RedissonSuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3NearCache
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3NearCacheConfig
import io.bluetape4k.cache.nearcache.ResilientRedissonResp3SuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCache
import io.bluetape4k.logging.KLogging
import io.lettuce.core.RedisClient
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson 기반 캐시 인스턴스를 생성하는 팩토리 오브젝트입니다.
 *
 * [JCache], [SuspendCache], [NearCache], [SuspendNearCache],
 * RESP3 하이브리드 NearCache, Resilient NearCache를 한 곳에서 생성할 수 있습니다.
 *
 * ```kotlin
 * val cache = RedissonCaches.jcache<String, String>("my-cache", redisson)
 * val near  = RedissonCaches.nearCache<String, String>("my-near", redisson)
 * val resp3 = RedissonCaches.resp3NearCache<String>(redisson, redisClient)
 * ```
 */
object RedissonCaches : KLogging() {

    // ─────────────────────────────────────────────
    // JCache
    // ─────────────────────────────────────────────

    /**
     * RedissonClient로 [JCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param redisson Redisson 클라이언트
     * @param configuration JCache 설정
     * @return [JCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> jcache(
        cacheName: String,
        redisson: RedissonClient,
        configuration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
    ): JCache<K, V> = RedissonJCaching.getOrCreate(cacheName, redisson, configuration)

    /**
     * Redisson [Config]로 [JCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param redissonConfig Redisson 설정
     * @param configuration JCache 설정
     * @return [JCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> jcache(
        cacheName: String,
        redissonConfig: Config,
        configuration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
    ): JCache<K, V> = RedissonJCaching.getOrCreate(cacheName, redissonConfig, configuration)

    // ─────────────────────────────────────────────
    // SuspendCache
    // ─────────────────────────────────────────────

    /**
     * RedissonClient로 [RedissonSuspendCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param redisson Redisson 클라이언트
     * @param configuration JCache 설정
     * @return [RedissonSuspendCache] 인스턴스
     */
    fun <K : Any, V : Any> suspendCache(
        cacheName: String,
        redisson: RedissonClient,
        configuration: Configuration<K, V> = MutableConfiguration(),
    ): RedissonSuspendCache<K, V> = RedissonSuspendCache(cacheName, redisson, configuration)

    /**
     * Redisson [Config]로 [RedissonSuspendCache]를 생성하거나 재사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param cacheName 캐시 이름
     * @param redissonConfig Redisson 설정
     * @return [RedissonSuspendCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> suspendCache(
        cacheName: String,
        redissonConfig: Config,
    ): RedissonSuspendCache<K, V> = RedissonSuspendCache(cacheName, redissonConfig)

    // ─────────────────────────────────────────────
    // NearCache (JCache 백엔드)
    // ─────────────────────────────────────────────

    /**
     * 기존 [JCache] 인스턴스로 [NearCache]를 생성합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param backCache 백엔드 JCache
     * @param nearCacheConfig Near Cache 설정
     * @return [NearCache] 인스턴스
     */
    fun <K : Any, V : Any> nearCache(
        backCache: JCache<K, V>,
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = RedissonNearCache(backCache, nearCacheConfig)

    /**
     * RedissonClient로 백엔드 캐시를 생성하고 [NearCache]를 반환합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param backCacheName 백엔드 캐시 이름
     * @param redisson Redisson 클라이언트
     * @param backCacheConfiguration 백엔드 JCache 설정
     * @param nearCacheConfig Near Cache 설정
     * @return [NearCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> nearCache(
        backCacheName: String,
        redisson: RedissonClient,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = RedissonNearCache(backCacheName, redisson, backCacheConfiguration, nearCacheConfig)

    // ─────────────────────────────────────────────
    // SuspendNearCache (JCache 백엔드)
    // ─────────────────────────────────────────────

    /**
     * Front/Back [SuspendCache]를 직접 지정해 [SuspendNearCache]를 생성합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param frontSuspendCache 프론트 SuspendCache
     * @param backSuspendCache 백엔드 SuspendCache
     * @param checkExpiryPeriod 만료 검사 주기(ms)
     * @return [SuspendNearCache] 인스턴스
     */
    fun <K : Any, V : Any> suspendNearCache(
        frontSuspendCache: SuspendCache<K, V>,
        backSuspendCache: SuspendCache<K, V>,
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
    ): SuspendNearCache<K, V> = RedissonSuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

    /**
     * RedissonClient로 백엔드 캐시를 생성하고 [SuspendNearCache]를 반환합니다.
     * 프론트 캐시는 기본적으로 Caffeine을 사용합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param backCacheName 백엔드 캐시 이름
     * @param redisson Redisson 클라이언트
     * @param backCacheConfiguration 백엔드 JCache 설정
     * @param checkExpiryPeriod 만료 검사 주기(ms)
     * @param frontCacheBuilder 프론트 Caffeine 빌더 블록
     * @return [SuspendNearCache] 인스턴스
     */
    inline fun <reified K : Any, reified V : Any> suspendNearCache(
        backCacheName: String,
        redisson: RedissonClient,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): SuspendNearCache<K, V> =
        RedissonSuspendNearCache(backCacheName, redisson, backCacheConfiguration, checkExpiryPeriod, frontCacheBuilder)

    // ─────────────────────────────────────────────
    // RESP3 NearCache
    // ─────────────────────────────────────────────

    /**
     * Redisson + Lettuce RESP3 하이브리드 [RedissonResp3NearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param redisClient Lettuce RedisClient (RESP3 tracking 용)
     * @param codec Redisson 직렬화 Codec
     * @param config Near Cache 설정
     * @return [RedissonResp3NearCache] 인스턴스
     */
    fun <V : Any> resp3NearCache(
        redisson: RedissonClient,
        redisClient: RedisClient,
        codec: Codec = RedissonNearCache.defaultNearCacheCodec,
        config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
    ): RedissonResp3NearCache<V> = RedissonResp3NearCache(redisson, redisClient, codec, config)

    /**
     * Redisson + Lettuce RESP3 하이브리드 [RedissonResp3SuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param redisClient Lettuce RedisClient (RESP3 tracking 용)
     * @param codec Redisson 직렬화 Codec
     * @param config Near Cache 설정
     * @return [RedissonResp3SuspendNearCache] 인스턴스
     */
    fun <V : Any> resp3SuspendNearCache(
        redisson: RedissonClient,
        redisClient: RedisClient,
        codec: Codec = RedissonNearCache.defaultNearCacheCodec,
        config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
    ): RedissonResp3SuspendNearCache<V> = RedissonResp3SuspendNearCache(redisson, redisClient, codec, config)

    // ─────────────────────────────────────────────
    // Resilient RESP3 NearCache
    // ─────────────────────────────────────────────

    /**
     * write-behind + retry를 지원하는 [ResilientRedissonResp3NearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param redisClient Lettuce RedisClient (RESP3 tracking 용)
     * @param codec Redisson 직렬화 Codec
     * @param config Resilient Near Cache 설정
     * @return [ResilientRedissonResp3NearCache] 인스턴스
     */
    fun <V : Any> resilientResp3NearCache(
        redisson: RedissonClient,
        redisClient: RedisClient,
        codec: Codec = RedissonNearCache.defaultNearCacheCodec,
        config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(RedissonResp3NearCacheConfig()),
    ): ResilientRedissonResp3NearCache<V> = ResilientRedissonResp3NearCache(redisson, redisClient, codec, config)

    /**
     * write-behind + retry를 지원하는 [ResilientRedissonResp3SuspendNearCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param redisClient Lettuce RedisClient (RESP3 tracking 용)
     * @param codec Redisson 직렬화 Codec
     * @param config Resilient Near Cache 설정
     * @return [ResilientRedissonResp3SuspendNearCache] 인스턴스
     */
    fun <V : Any> resilientResp3SuspendNearCache(
        redisson: RedissonClient,
        redisClient: RedisClient,
        codec: Codec = RedissonNearCache.defaultNearCacheCodec,
        config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(RedissonResp3NearCacheConfig()),
    ): ResilientRedissonResp3SuspendNearCache<V> =
        ResilientRedissonResp3SuspendNearCache(redisson, redisClient, codec, config)
}
