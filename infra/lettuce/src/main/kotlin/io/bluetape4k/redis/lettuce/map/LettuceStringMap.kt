package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 분산 Map(Distributed Map) 구현체입니다.
 *
 * Redis의 Hash 자료구조(HSET/HGET/HDEL 등)를 사용하여 분산 Map을 구현합니다.
 * 동기(sync)와 비동기(CompletableFuture) 두 가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendMap]을 사용하세요.
 *
 * ```kotlin
 * val codec = LettuceBinaryCodecs.lz4Fory<MyData>()
 * val connection = redisClient.connect(codec)
 * val map = LettuceMap<MyData>(connection, "my-map")
 *
 * // 동기 방식
 * map.put("key", myData)
 * val value = map.get("key")
 *
 * // 비동기 방식
 * map.putAsync("key", myData)
 * val future = map.getAsync("key")
 * ```
 *
 * @param V 값 타입
 * @param connection Lettuce StatefulRedisConnection (LettuceBinaryCodec<V> 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
class LettuceStringMap(
    private val connection: StatefulRedisConnection<String, String>,
    mapKey: String,
): LettuceMap<String>(connection, mapKey) {

    companion object: KLogging()

}
