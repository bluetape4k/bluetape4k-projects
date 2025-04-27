package io.bluetape4k.exposed.redisson

import io.bluetape4k.redis.redisson.RedissonCodecs
import org.redisson.client.codec.Codec
import java.io.Serializable
import java.time.Duration

data class ExposedRedisCacheConfig(
    val readThrough: Boolean = true,
    val writeThrough: Boolean = true,
    val codec: Codec = RedissonCodecs.ZstdFury,
    val retryAttempts: Int = 3,
    val retryInterval: Duration = Duration.ofMillis(10),
    val timeToLive: Duration = Duration.ofMinutes(10),
): Serializable {
    companion object {
        val DEFAULT = ExposedRedisCacheConfig()

        val READ_THROUGH = ExposedRedisCacheConfig(
            readThrough = true,
            writeThrough = false,
        )
        val WRITE_THROUGH = ExposedRedisCacheConfig(
            readThrough = false,
            writeThrough = true,
        )

        val READ_WRITE_THROUGH = DEFAULT
    }
}
