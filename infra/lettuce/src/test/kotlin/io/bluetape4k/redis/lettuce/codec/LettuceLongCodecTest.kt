package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class LettuceLongCodecTest: AbstractLettuceTest() {

    companion object: KLogging()

    private val codec = LettuceLongCodec

    @Test
    fun `encodeValue 와 decodeValue 가 round-trip 을 보장한다`() {
        val values = listOf(0L, 1L, -1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE, 100L, -100L)
        for (value in values) {
            val encoded = codec.encodeValue(value)
            val decoded = codec.decodeValue(encoded)
            decoded shouldBeEqualTo value
        }
    }

    @Test
    fun `encodeKey 와 decodeKey 가 round-trip 을 보장한다`() {
        val keys = listOf("key", "my-key", "ns:key:1", randomName())
        for (key in keys) {
            val encoded = codec.encodeKey(key)
            val decoded = codec.decodeKey(encoded)
            decoded shouldBeEqualTo key
        }
    }

    @Test
    fun `encodeValue 는 8바이트 big-endian ByteBuffer 를 반환한다`() {
        val buf = codec.encodeValue(0x0102030405060708L)
        buf.limit() shouldBeEqualTo Long.SIZE_BYTES
        buf.get() shouldBeEqualTo 0x01.toByte()
        buf.get() shouldBeEqualTo 0x02.toByte()
        buf.get() shouldBeEqualTo 0x03.toByte()
        buf.get() shouldBeEqualTo 0x04.toByte()
        buf.get() shouldBeEqualTo 0x05.toByte()
        buf.get() shouldBeEqualTo 0x06.toByte()
        buf.get() shouldBeEqualTo 0x07.toByte()
        buf.get() shouldBeEqualTo 0x08.toByte()
    }

    @Test
    fun `decodeValue 는 caller 의 ByteBuffer position 을 변경하지 않는다`() {
        val value = 123456789L
        val buf = codec.encodeValue(value)
        val originalPosition = buf.position()

        codec.decodeValue(buf) shouldBeEqualTo value
        buf.position() shouldBeEqualTo originalPosition
    }

    @Test
    fun `Redis 에 Long 값을 저장하고 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()
            val key = randomName()

            commands.set(key, 42L)
            commands.get(key) shouldBeEqualTo 42L

            commands.del(key)
        }
    }

    @Test
    fun `Redis Hash 에 Long 값을 hset 으로 저장하고 hgetall 로 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()
            val key = randomName()
            val originMap = mapOf(
                "zero" to 0L,
                "one" to 1L,
                "min" to Long.MIN_VALUE,
                "max" to Long.MAX_VALUE,
                "neg" to -999L,
            )

            commands.hset(key, originMap)
            commands.hgetall(key) shouldContainSame originMap

            commands.del(key)
        }
    }

    @Test
    fun `경계값 Long 을 Redis 에 저장하고 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            listOf(Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L).forEach { value ->
                val key = randomName()
                commands.set(key, value)
                commands.get(key) shouldBeEqualTo value
                commands.del(key)
            }
        }
    }
}
