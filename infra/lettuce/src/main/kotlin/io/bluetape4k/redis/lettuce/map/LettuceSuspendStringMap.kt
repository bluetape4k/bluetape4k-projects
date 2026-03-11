package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 분산 Map의 코루틴 구현체입니다.
 *
 * [LettuceMap]의 코루틴(suspend) 버전으로, Redis Hash 자료구조(HSET/HGET/HDEL 등)를
 * suspend 함수를 통해 사용합니다.
 * 동기/비동기(CompletableFuture) 방식은 [LettuceMap]을 사용하세요.
 *
 * ```kotlin
 * val codec = LettuceBinaryCodecs.lz4Fory<MyData>()
 * val connection = redisClient.connect(codec)
 * val suspendMap = LettuceSuspendMap<MyData>(connection, "my-map")
 *
 * val value = suspendMap.get("key")
 * suspendMap.put("key", myData)
 * ```
 *
 * @param V 값 타입
 * @param connection Lettuce StatefulRedisConnection (LettuceBinaryCodec<V> 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
class LettuceSuspendStringMap<V: Any>(
    private val connection: StatefulRedisConnection<String, String>,
    mapKey: String,
): LettuceSuspendMap<String>(connection, mapKey) {

    companion object: KLogging()

}
