package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 분산 String Map의 코루틴 구현체입니다.
 *
 * [LettuceSuspendMap]`<String>`을 상속하여 `StatefulRedisConnection<String, String>` 기반으로 동작합니다.
 * 동기/비동기(CompletableFuture) 방식은 [LettuceStringMap]을 사용하세요.
 *
 * ```kotlin
 * val connection = redisClient.connect()
 * val suspendMap = LettuceSuspendStringMap(connection, "my-string-map")
 *
 * val value = suspendMap.get("key")
 * suspendMap.put("key", "value")
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
class LettuceSuspendStringMap(
    private val connection: StatefulRedisConnection<String, String>,
    mapKey: String,
): LettuceSuspendMap<String>(connection, mapKey) {
    companion object: KLogging()
}
