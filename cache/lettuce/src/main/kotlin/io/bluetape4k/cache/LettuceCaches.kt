package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.LettuceJCache
import io.bluetape4k.cache.jcache.LettuceJCaching
import io.bluetape4k.cache.jcache.LettuceSuspendJCache
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBuilder
import io.bluetape4k.cache.nearcache.jcache.SuspendNearJCache
import io.bluetape4k.cache.nearcache.jcache.nearJCacheConfig
import io.bluetape4k.cache.nearcache.lettuceNearCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec
import java.util.concurrent.TimeUnit

/**
 * Lettuce 기반 캐시 팩토리 object.
 *
 * JCache, NearCache, SuspendNearCache, ResilientNearCache, ResilientSuspendNearCache를
 * 간편하게 생성할 수 있는 함수를 제공합니다.
 *
 * ```kotlin
 * val nearCache = LettuceCaches.nearCache<String>(redisClient) {
 *     cacheName = "my-cache"
 *     redisTtl = Duration.ofMinutes(10)
 * }
 * ```
 */
object LettuceCaches: KLogging() {
    // -------------------------------------------------------------------------
    // JCache
    // -------------------------------------------------------------------------

    /**
     * Lettuce 기반 [JCache]`<K, V>`를 생성하거나 기존 캐시를 가져옵니다.
     *
     * @param cacheName 캐시 이름
     * @param ttlSeconds TTL (초), null이면 만료 없음
     * @param codec 직렬화 codec (기본값: lz4Fory)
     */
    inline fun <reified K: Any, reified V: Any> jcache(
        redisClient: RedisClient,
        cacheName: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
    ): JCache<K, V> = LettuceJCaching.getOrCreate(redisClient, cacheName, ttlSeconds, codec)

    // -------------------------------------------------------------------------
    // SuspendJCache
    // -------------------------------------------------------------------------

    /**
     * Lettuce 기반 [LettuceSuspendJCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param cacheName 캐시 이름
     * @param ttlSeconds TTL (초), null이면 만료 없음
     * @param codec 직렬화 codec (기본값: lz4Fory)
     */
    inline fun <reified V: Any> suspendJCache(
        redisClient: RedisClient,
        cacheName: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
    ): LettuceSuspendJCache<V> {
        val jcache = jcache<String, V>(redisClient, cacheName, ttlSeconds, codec)
        return LettuceSuspendJCache(jcache as LettuceJCache<String, V>)
    }

    // -------------------------------------------------------------------------
    // NearJCache (JCache 기반)
    // -------------------------------------------------------------------------

    /**
     * DSL 블록으로 Lettuce 기반 [NearJCache]를 생성합니다.
     *
     * ```kotlin
     * val cache = LettuceCaches.nearJCache<String, String>(redisClient) {
     *     cacheName = "my-near-jcache"
     *     isSynchronous = true
     * }
     * ```
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param redisClient Lettuce RedisClient
     * @param codec 직렬화 codec (기본값: lz4Fory)
     * @param block [NearJCacheConfigBuilder] DSL 블록
     */
    inline fun <reified K: Any, reified V: Any> nearJCache(
        redisClient: RedisClient,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
        block: NearJCacheConfigBuilder<K, V>.() -> Unit,
    ): NearJCache<K, V> {
        val config = nearJCacheConfig(block)
        return nearJCache(redisClient, config, codec)
    }

    /**
     * [NearJCacheConfig]로 Lettuce 기반 [NearJCache]를 생성합니다.
     *
     * @param K 키 타입
     * @param V 값 타입
     * @param redisClient Lettuce RedisClient
     * @param config [NearJCacheConfig] 설정
     * @param codec 직렬화 codec (기본값: lz4Fory)
     */
    inline fun <reified K: Any, reified V: Any> nearJCache(
        redisClient: RedisClient,
        config: NearJCacheConfig<K, V>,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
    ): NearJCache<K, V> {
        val backCache = jcache<K, V>(redisClient, config.cacheName, codec = codec)
        log.info { "Lettuce NearJCache 생성. cacheName=${config.cacheName}" }
        return NearJCache(config, backCache)
    }

    // -------------------------------------------------------------------------
    // SuspendNearJCache (JCache 기반, 코루틴)
    // -------------------------------------------------------------------------

    /**
     * DSL 블록으로 Lettuce 기반 [SuspendNearJCache]를 생성합니다.
     *
     * ```kotlin
     * val cache = LettuceCaches.suspendNearJCache<String>(redisClient) {
     *     cacheName = "my-suspend-near-jcache"
     * }
     * ```
     *
     * @param V 값 타입
     * @param redisClient Lettuce RedisClient
     * @param codec 직렬화 codec (기본값: lz4Fory)
     * @param block [NearJCacheConfigBuilder] DSL 블록
     */
    inline fun <reified V: Any> suspendNearJCache(
        redisClient: RedisClient,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
        block: NearJCacheConfigBuilder<String, V>.() -> Unit,
    ): SuspendNearJCache<String, V> {
        val config = nearJCacheConfig(block)
        return suspendNearJCache(redisClient, config, codec)
    }

    /**
     * [NearJCacheConfig]로 Lettuce 기반 [SuspendNearJCache]를 생성합니다.
     *
     * @param V 값 타입
     * @param redisClient Lettuce RedisClient
     * @param config [NearJCacheConfig] 설정
     * @param codec 직렬화 codec (기본값: lz4Fory)
     */
    inline fun <reified V: Any> suspendNearJCache(
        redisClient: RedisClient,
        config: NearJCacheConfig<String, V>,
        codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.default<V>(),
    ): SuspendNearJCache<String, V> {
        val backJCache = jcache<String, V>(redisClient, config.cacheName, codec = codec)
        val backCache = LettuceSuspendJCache(backJCache as LettuceJCache<String, V>)

        val frontCache = CaffeineSuspendJCache<String, V> {
            maximumSize(10_000)
            expireAfterAccess(30, TimeUnit.MINUTES)
        }

        log.info { "Lettuce SuspendNearJCache 생성. cacheName=${config.cacheName}" }
        return SuspendNearJCache(frontCache, backCache)
    }

    // -------------------------------------------------------------------------
    // NearCache (동기)
    // -------------------------------------------------------------------------

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param config NearCache 설정
     */
    fun <V: Any> nearCache(
        redisClient: RedisClient,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): NearCacheOperations<V> = LettuceNearCache(redisClient, config = config)

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param codec Redis 코덱
     * @param config NearCache 설정
     */
    fun <V: Any> nearCache(
        redisClient: RedisClient,
        codec: RedisCodec<String, V>,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): NearCacheOperations<V> = LettuceNearCache(redisClient, codec, config)

    /**
     * DSL 빌더를 이용해 [LettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block NearCache 설정 DSL 블록
     */
    fun <V: Any> nearCache(
        redisClient: RedisClient,
        block: LettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): NearCacheOperations<V> {
        val config = lettuceNearCacheConfig(block)
        return LettuceNearCache(redisClient, config = config)
    }

    // -------------------------------------------------------------------------
    // SuspendNearCache (코루틴)
    // -------------------------------------------------------------------------

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param config NearCache 설정
     */
    fun <V: Any> suspendNearCache(
        redisClient: RedisClient,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): SuspendNearCacheOperations<V> =
        LettuceSuspendNearCache(redisClient, config = config)

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param codec Redis 코덱
     * @param config NearCache 설정
     */
    fun <V: Any> suspendNearCache(
        redisClient: RedisClient,
        codec: RedisCodec<String, V>,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): SuspendNearCacheOperations<V> =
        LettuceSuspendNearCache(redisClient, codec, config)

    /**
     * DSL 빌더를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block NearCache 설정 DSL 블록
     */
    fun <V: Any> suspendNearCache(
        redisClient: RedisClient,
        block: LettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): SuspendNearCacheOperations<V> {
        val config = lettuceNearCacheConfig(block)
        return LettuceSuspendNearCache(
            redisClient,
            LettuceBinaryCodecs.default<V>(),
            config
        )
    }
}
