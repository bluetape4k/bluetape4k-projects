package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.LettuceJCaching
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.NearCacheOperations
import io.bluetape4k.cache.nearcache.SuspendNearCacheOperations
import io.bluetape4k.cache.nearcache.lettuceNearCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec

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
object LettuceCaches : KLogging() {
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
    inline fun <reified K : Any, reified V : Any> jcache(
        redisClient: RedisClient,
        cacheName: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory<Any>(),
    ): JCache<K, V> = LettuceJCaching.getOrCreate(redisClient, cacheName, ttlSeconds, codec)

    // -------------------------------------------------------------------------
    // NearCache (동기)
    // -------------------------------------------------------------------------

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param config NearCache 설정
     */
    fun <V : Any> nearCache(
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
    fun <V : Any> nearCache(
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
    fun <V : Any> nearCache(
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
    fun <V : Any> suspendNearCache(
        redisClient: RedisClient,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): SuspendNearCacheOperations<V> = LettuceSuspendNearCache(redisClient, config = config)

    /**
     * [LettuceNearCacheConfig]를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param codec Redis 코덱
     * @param config NearCache 설정
     */
    fun <V : Any> suspendNearCache(
        redisClient: RedisClient,
        codec: RedisCodec<String, V>,
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): SuspendNearCacheOperations<V> = LettuceSuspendNearCache(redisClient, codec, config)

    /**
     * DSL 빌더를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block NearCache 설정 DSL 블록
     */
    fun <V : Any> suspendNearCache(
        redisClient: RedisClient,
        block: LettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): SuspendNearCacheOperations<V> {
        val config = lettuceNearCacheConfig(block)
        return LettuceSuspendNearCache(redisClient, LettuceBinaryCodecs.lz4Fory(), config)
    }
}
