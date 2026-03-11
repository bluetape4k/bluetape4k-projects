package io.bluetape4k.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.LettuceJCaching
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.LettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.ResilientLettuceNearCache
import io.bluetape4k.cache.nearcache.ResilientLettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.ResilientLettuceNearCacheConfigBuilder
import io.bluetape4k.cache.nearcache.ResilientLettuceSuspendNearCache
import io.bluetape4k.cache.nearcache.lettuceNearCacheConfig
import io.bluetape4k.cache.nearcache.resilientLettuceNearCacheConfig
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
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
     * @param serializer 직렬화 방식 (기본값: Fory)
     */
    inline fun <reified K : Any, reified V : Any> jcache(
        redisClient: RedisClient,
        cacheName: String,
        ttlSeconds: Long? = null,
        serializer: BinarySerializer = BinarySerializers.Fory,
    ): JCache<K, V> = LettuceJCaching.getOrCreate(redisClient, cacheName, ttlSeconds, serializer)

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
    ): LettuceNearCache<V> = LettuceNearCache(redisClient, config = config)

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
    ): LettuceNearCache<V> = LettuceNearCache(redisClient, codec, config)

    /**
     * DSL 빌더를 이용해 [LettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block NearCache 설정 DSL 블록
     */
    fun <V : Any> nearCache(
        redisClient: RedisClient,
        block: LettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): LettuceNearCache<V> {
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
    ): LettuceSuspendNearCache<V> = LettuceSuspendNearCache(redisClient, config = config)

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
    ): LettuceSuspendNearCache<V> = LettuceSuspendNearCache(redisClient, codec, config)

    /**
     * DSL 빌더를 이용해 [LettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block NearCache 설정 DSL 블록
     */
    fun <V : Any> suspendNearCache(
        redisClient: RedisClient,
        block: LettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): LettuceSuspendNearCache<V> {
        val config = lettuceNearCacheConfig(block)
        return LettuceSuspendNearCache(redisClient, LettuceBinaryCodecs.lz4Fory(), config)
    }

    // -------------------------------------------------------------------------
    // ResilientNearCache (동기 + 재시도)
    // -------------------------------------------------------------------------

    /**
     * [ResilientLettuceNearCacheConfig]를 이용해 [ResilientLettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param config Resilient NearCache 설정
     */
    fun <V : Any> resilientNearCache(
        redisClient: RedisClient,
        config: ResilientLettuceNearCacheConfig<String, V> = ResilientLettuceNearCacheConfig(LettuceNearCacheConfig()),
    ): ResilientLettuceNearCache<V> = ResilientLettuceNearCache(redisClient, config = config)

    /**
     * [LettuceNearCacheConfig]로부터 [ResilientLettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param nearCacheConfig NearCache 기본 설정
     */
    fun <V : Any> resilientNearCache(
        redisClient: RedisClient,
        nearCacheConfig: LettuceNearCacheConfig<String, V>,
    ): ResilientLettuceNearCache<V> =
        ResilientLettuceNearCache(redisClient, config = ResilientLettuceNearCacheConfig(nearCacheConfig))

    /**
     * DSL 빌더를 이용해 [ResilientLettuceNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block Resilient NearCache 설정 DSL 블록
     */
    fun <V : Any> resilientNearCache(
        redisClient: RedisClient,
        block: ResilientLettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): ResilientLettuceNearCache<V> {
        val config = resilientLettuceNearCacheConfig(block)
        return ResilientLettuceNearCache(redisClient, config = config)
    }

    // -------------------------------------------------------------------------
    // ResilientSuspendNearCache (코루틴 + 재시도)
    // -------------------------------------------------------------------------

    /**
     * [ResilientLettuceNearCacheConfig]를 이용해 [ResilientLettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param config Resilient NearCache 설정
     */
    fun <V : Any> resilientSuspendNearCache(
        redisClient: RedisClient,
        config: ResilientLettuceNearCacheConfig<String, V> = ResilientLettuceNearCacheConfig(LettuceNearCacheConfig()),
    ): ResilientLettuceSuspendNearCache<V> = ResilientLettuceSuspendNearCache(redisClient, config = config)

    /**
     * [LettuceNearCacheConfig]로부터 [ResilientLettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param nearCacheConfig NearCache 기본 설정
     */
    fun <V : Any> resilientSuspendNearCache(
        redisClient: RedisClient,
        nearCacheConfig: LettuceNearCacheConfig<String, V>,
    ): ResilientLettuceSuspendNearCache<V> =
        ResilientLettuceSuspendNearCache(redisClient, config = ResilientLettuceNearCacheConfig(nearCacheConfig))

    /**
     * DSL 빌더를 이용해 [ResilientLettuceSuspendNearCache]`<V>`를 생성합니다.
     *
     * @param redisClient Lettuce RedisClient
     * @param block Resilient NearCache 설정 DSL 블록
     */
    fun <V : Any> resilientSuspendNearCache(
        redisClient: RedisClient,
        block: ResilientLettuceNearCacheConfigBuilder<String, V>.() -> Unit,
    ): ResilientLettuceSuspendNearCache<V> {
        val config = resilientLettuceNearCacheConfig(block)
        return ResilientLettuceSuspendNearCache(redisClient, config = config)
    }
}
