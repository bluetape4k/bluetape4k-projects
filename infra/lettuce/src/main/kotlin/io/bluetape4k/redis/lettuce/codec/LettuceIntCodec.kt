package io.bluetape4k.redis.lettuce.codec

import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Int 값을 4바이트 big-endian 으로 직렬화/역직렬화하는 Lettuce [RedisCodec] 구현체입니다.
 *
 * Redisson의 IntegerCodec과 동일한 역할을 합니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceIntCodec)
 * val map = LettuceMap<Int>(connection, "my-int-map")
 * ```
 */
object LettuceIntCodec: RedisCodec<String, Int> {

    override fun decodeKey(bytes: ByteBuffer): String =
        StandardCharsets.UTF_8.decode(bytes.duplicate()).toString()

    override fun decodeValue(bytes: ByteBuffer): Int =
        bytes.duplicate().getInt()

    override fun encodeKey(key: String): ByteBuffer =
        ByteBuffer.wrap(key.toByteArray(StandardCharsets.UTF_8))

    override fun encodeValue(value: Int): ByteBuffer {
        val buf = ByteBuffer.allocate(Int.SIZE_BYTES)
        buf.putInt(value)
        buf.flip()
        return buf
    }
}
