package io.bluetape4k.redis.lettuce.codec

import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Long 값을 8바이트 big-endian 으로 직렬화/역직렬화하는 Lettuce [RedisCodec] 구현체입니다.
 *
 * Redisson의 LongCodec과 동일한 역할을 합니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceMap<Long>(connection, "my-long-map")
 * ```
 */
object LettuceLongCodec: RedisCodec<String, Long> {

    override fun decodeKey(bytes: ByteBuffer): String =
        StandardCharsets.UTF_8.decode(bytes.duplicate()).toString()

    override fun decodeValue(bytes: ByteBuffer): Long =
        bytes.duplicate().getLong()

    override fun encodeKey(key: String): ByteBuffer =
        ByteBuffer.wrap(key.toByteArray(StandardCharsets.UTF_8))

    override fun encodeValue(value: Long): ByteBuffer {
        val buf = ByteBuffer.allocate(Long.SIZE_BYTES)
        buf.putLong(value)
        buf.flip()
        return buf
    }
}
