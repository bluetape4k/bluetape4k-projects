package io.bluetape4k.cache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.RedissonJCaching
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.RedissonNearCache
import io.bluetape4k.cache.nearcache.RedissonNearCacheConfig
import io.bluetape4k.cache.nearcache.RedissonSuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCache
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson 기반 캐시 인스턴스를 생성하는 팩토리 오브젝트입니다.
 *
 * [JCache], [SuspendCache], [NearCache], [SuspendNearCache],
 * [NearCacheOperations], [SuspendNearCacheOperations]를 한 곳에서 생성할 수 있습니다.
 *
 * ```kotlin
 * val cache = RedissonCaches.jcache<String, String>("my-cache", redisson)
 * val near  = RedissonCaches.nearCacheOps<String>("my-near", redisson)
 * val suspendNear = RedissonCaches.suspendNearCacheOps<String>("my-near", redisson)
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
        configuration: Configuration<K, V> =
            MutableConfiguration<K, V>().apply {
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
        configuration: Configuration<K, V> =
            MutableConfiguration<K, V>().apply {
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
    // NearCache (JCache 백엔드, 레거시)
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
    ): NearCache<K, V> = NearCache(nearCacheConfig, backCache)

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
        backCacheConfiguration: Configuration<K, V> =
            MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> {
        val backCache = RedissonJCaching.getOrCreate(backCacheName, redisson, backCacheConfiguration)
        return NearCache(nearCacheConfig, backCache)
    }

    // ─────────────────────────────────────────────
    // SuspendNearCache (JCache 백엔드, 레거시)
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
    ): SuspendNearCache<K, V> = SuspendNearCache(frontSuspendCache, backSuspendCache, checkExpiryPeriod)

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
        backCacheConfiguration: Configuration<K, V> =
            MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            },
        checkExpiryPeriod: Long = SuspendNearCache.DEFAULT_EXPIRY_CHECK_PERIOD,
        noinline frontCacheBuilder: Caffeine<Any, Any>.() -> Unit = {},
    ): SuspendNearCache<K, V> {
        val frontCache =
            io.bluetape4k.cache.jcache
                .CaffeineSuspendCache<K, V>(frontCacheBuilder)
        val backCache = RedissonSuspendCache<K, V>(backCacheName, redisson, backCacheConfiguration)
        return SuspendNearCache(frontCache, backCache, checkExpiryPeriod)
    }

    // ─────────────────────────────────────────────
    // NearCacheOperations (RLocalCachedMap 기반)
    // ─────────────────────────────────────────────

    /**
     * Redisson [RLocalCachedMap][org.redisson.api.RLocalCachedMap] 기반 [NearCacheOperations]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param config Near Cache 설정
     * @param codec Redisson 직렬화 Codec
     * @return [NearCacheOperations] 인스턴스
     */
    fun <V : Any> nearCacheOps(
        redisson: RedissonClient,
        config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
        codec: Codec = RedissonCodecs.LZ4Fory,
    ): NearCacheOperations<V> = RedissonNearCache(redisson, config, codec)

    /**
     * Redisson [RLocalCachedMap][org.redisson.api.RLocalCachedMap] 기반 [SuspendNearCacheOperations]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisson Redisson 클라이언트
     * @param config Near Cache 설정
     * @param codec Redisson 직렬화 Codec
     * @return [SuspendNearCacheOperations] 인스턴스
     */
    fun <V : Any> suspendNearCacheOps(
        redisson: RedissonClient,
        config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
        codec: Codec = RedissonCodecs.LZ4Fory,
    ): SuspendNearCacheOperations<V> = RedissonSuspendNearCache(redisson, config, codec)
}
