package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.lettuceDefaultCodec
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec

/**
 * Lettuce Near Cache 팩토리 함수 모음.
 *
 * [NearCacheOperations] 또는 [SuspendNearCacheOperations] 인터페이스 타입으로
 * 구현체를 생성한다.
 *
 * ```kotlin
 * // blocking
 * val cache: NearCacheOperations<String> =
 *     lettuceNearCacheOf(redisClient, codec, config)
 *
 * // suspend
 * val cache: SuspendNearCacheOperations<String> =
 *     lettuceSuspendNearCacheOf(redisClient, codec, config)
 * ```
 */

/**
 * 동기(Blocking) Lettuce Near Cache를 생성한다.
 *
 * @param redisClient Lettuce Redis 클라이언트
 * @param codec Redis 키/값 직렬화 Codec
 * @param config Near Cache 설정
 * @return [NearCacheOperations] 구현체
 */
fun <V : Any> lettuceNearCacheOf(
    redisClient: RedisClient,
    codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
): NearCacheOperations<V> = LettuceNearCache(redisClient, codec, config)

/**
 * Coroutine(Suspend) Lettuce Near Cache를 생성한다.
 *
 * @param redisClient Lettuce Redis 클라이언트
 * @param codec Redis 키/값 직렬화 Codec
 * @param config Near Cache 설정
 * @return [SuspendNearCacheOperations] 구현체
 */
fun <V : Any> lettuceSuspendNearCacheOf(
    redisClient: RedisClient,
    codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
): SuspendNearCacheOperations<V> = LettuceSuspendNearCache(redisClient, codec, config)
