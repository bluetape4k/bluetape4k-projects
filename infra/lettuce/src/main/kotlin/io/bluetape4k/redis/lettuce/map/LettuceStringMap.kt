package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 분산 String Map 구현체입니다.
 *
 * [LettuceMap]`<String>`을 상속하여 `StatefulRedisConnection<String, String>` 기반으로 동작합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendStringMap]을 사용하세요.
 *
 * ```kotlin
 * val connection = redisClient.connect()
 * val map = LettuceStringMap(connection, "my-string-map")
 *
 * map.put("key", "value")
 * val value = map.get("key")
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
class LettuceStringMap(
    private val connection: StatefulRedisConnection<String, String>,
    mapKey: String,
) : LettuceMap<String>(connection, mapKey) {
    companion object : KLogging()
}
