package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.lettuceDefaultCodec
import io.bluetape4k.logging.KLogging
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec
import java.time.Duration

/**
 * Lettuce Near Cache 팩토리.
 *
 * [LettuceNearCacheOperations] 또는 [LettuceSuspendNearCacheOperations] 인터페이스 타입으로
 * 기본/Resilient 구현체를 생성한다.
 *
 * ```kotlin
 * // 기본 blocking
 * val cache: LettuceNearCacheOperations<String> =
 *     LettuceNearCacheFactory.blocking(redisClient, codec, config)
 *
 * // Resilient suspend
 * val cache: LettuceSuspendNearCacheOperations<String> =
 *     LettuceNearCacheFactory.resilientSuspend(redisClient, codec, config)
 *
 * // withRetry 단축
 * val cache: LettuceNearCacheOperations<String> =
 *     LettuceNearCacheFactory.withRetry(redisClient, codec, baseConfig) {
 *         retryMaxAttempts = 5
 *     }
 * ```
 */
object LettuceNearCacheFactory: KLogging() {
    /**
     * 기본 동기(Blocking) Near Cache를 생성한다.
     */
    fun <V: Any> blocking(
        redisClient: RedisClient,
        codec: RedisCodec<String, V> = lettuceDefaultCodec(),
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): LettuceNearCacheOperations<V> = LettuceNearCache(redisClient, codec, config)

    /**
     * 기본 Coroutine(Suspend) Near Cache를 생성한다.
     */
    fun <V: Any> suspend(
        redisClient: RedisClient,
        codec: RedisCodec<String, V> = lettuceDefaultCodec(),
        config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
    ): LettuceSuspendNearCacheOperations<V> = LettuceSuspendNearCache(redisClient, codec, config)

    /**
     * Resilient 동기(Blocking) Near Cache를 생성한다.
     */
    fun <V: Any> resilientBlocking(
        redisClient: RedisClient,
        codec: RedisCodec<String, V> = lettuceDefaultCodec(),
        config: ResilientLettuceNearCacheConfig<String, V> = ResilientLettuceNearCacheConfig(LettuceNearCacheConfig()),
    ): LettuceNearCacheOperations<V> = ResilientLettuceNearCache(redisClient, codec, config)

    /**
     * Resilient Coroutine(Suspend) Near Cache를 생성한다.
     */
    fun <V: Any> resilientSuspend(
        redisClient: RedisClient,
        codec: RedisCodec<String, V> = lettuceDefaultCodec(),
        config: ResilientLettuceNearCacheConfig<String, V> = ResilientLettuceNearCacheConfig(LettuceNearCacheConfig()),
    ): LettuceSuspendNearCacheOperations<V> = ResilientLettuceSuspendNearCache(redisClient, codec, config)
}

/**
 * 기본 [LettuceNearCacheConfig]에서 [ResilientLettuceNearCacheConfig]를 생성하는 헬퍼.
 */
fun <V: Any> LettuceNearCacheConfig<String, V>.withRetry(
    writeQueueCapacity: Int = 1024,
    retryMaxAttempts: Int = 3,
    retryWaitDuration: Duration = Duration.ofMillis(500),
    retryExponentialBackoff: Boolean = true,
    getFailureStrategy: GetFailureStrategy = GetFailureStrategy.RETURN_FRONT_OR_NULL,
): ResilientLettuceNearCacheConfig<String, V> =
    ResilientLettuceNearCacheConfig(
        base = this,
        writeQueueCapacity = writeQueueCapacity,
        retryMaxAttempts = retryMaxAttempts,
        retryWaitDuration = retryWaitDuration,
        retryExponentialBackoff = retryExponentialBackoff,
        getFailureStrategy = getFailureStrategy
    )

/**
 * 기본 config에서 resilient blocking Near Cache를 생성하는 단축 메서드.
 */
fun <V: Any> LettuceNearCacheFactory.withRetry(
    redisClient: RedisClient,
    codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    baseConfig: LettuceNearCacheConfig<String, V>,
    configure: ResilientLettuceNearCacheConfigBuilder<String, V>.() -> Unit = {},
): LettuceNearCacheOperations<V> {
    val builder =
        ResilientLettuceNearCacheConfigBuilder<String, V>()
            .apply {
                cacheName = baseConfig.cacheName
                maxLocalSize = baseConfig.maxLocalSize
                frontExpireAfterWrite = baseConfig.frontExpireAfterWrite
                frontExpireAfterAccess = baseConfig.frontExpireAfterAccess
                redisTtl = baseConfig.redisTtl
                useRespProtocol3 = baseConfig.useRespProtocol3
                recordStats = baseConfig.recordStats
            }.apply(configure)
    return resilientBlocking(redisClient, codec, builder.build())
}

/**
 * 기본 config에서 resilient suspend Near Cache를 생성하는 단축 메서드.
 */
fun <V: Any> LettuceNearCacheFactory.withRetrySuspend(
    redisClient: RedisClient,
    codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    baseConfig: LettuceNearCacheConfig<String, V>,
    configure: ResilientLettuceNearCacheConfigBuilder<String, V>.() -> Unit = {},
): LettuceSuspendNearCacheOperations<V> {
    val builder =
        ResilientLettuceNearCacheConfigBuilder<String, V>()
            .apply {
                cacheName = baseConfig.cacheName
                maxLocalSize = baseConfig.maxLocalSize
                frontExpireAfterWrite = baseConfig.frontExpireAfterWrite
                frontExpireAfterAccess = baseConfig.frontExpireAfterAccess
                redisTtl = baseConfig.redisTtl
                useRespProtocol3 = baseConfig.useRespProtocol3
                recordStats = baseConfig.recordStats
            }.apply(configure)
    return resilientSuspend(redisClient, codec, builder.build())
}
