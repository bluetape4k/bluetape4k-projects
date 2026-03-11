package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class LettuceIntCodecTest: AbstractLettuceTest() {

    companion object: KLogging()

    private val codec = LettuceIntCodec

    @Test
    fun `encodeValue 와 decodeValue 가 round-trip 을 보장한다`() {
        val values = listOf(0, 1, -1, 42, Int.MIN_VALUE, Int.MAX_VALUE, 100, -100)
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
    fun `encodeValue 는 4바이트 big-endian ByteBuffer 를 반환한다`() {
        val buf = codec.encodeValue(0x01020304)
        buf.limit() shouldBeEqualTo Int.SIZE_BYTES
        buf.get() shouldBeEqualTo 0x01.toByte()
        buf.get() shouldBeEqualTo 0x02.toByte()
        buf.get() shouldBeEqualTo 0x03.toByte()
        buf.get() shouldBeEqualTo 0x04.toByte()
    }

    @Test
    fun `decodeValue 는 caller 의 ByteBuffer position 을 변경하지 않는다`() {
        val value = 12345
        val buf = codec.encodeValue(value)
        val originalPosition = buf.position()

        codec.decodeValue(buf) shouldBeEqualTo value
        buf.position() shouldBeEqualTo originalPosition
    }

    @Test
    fun `Redis 에 Int 값을 저장하고 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()
            val key = randomName()

            commands.set(key, 42)
            commands.get(key) shouldBeEqualTo 42

            commands.del(key)
        }
    }

    @Test
    fun `Redis Hash 에 Int 값을 hset 으로 저장하고 hgetall 로 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()
            val key = randomName()
            val originMap = mapOf(
                "zero" to 0,
                "one" to 1,
                "min" to Int.MIN_VALUE,
                "max" to Int.MAX_VALUE,
                "neg" to -999,
            )

            commands.hset(key, originMap)
            commands.hgetall(key) shouldContainSame originMap

            commands.del(key)
        }
    }

    @Test
    fun `경계값 Int 를 Redis 에 저장하고 조회한다`() {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            listOf(Int.MIN_VALUE, Int.MAX_VALUE, 0, -1, 1).forEach { value ->
                val key = randomName()
                commands.set(key, value)
                commands.get(key) shouldBeEqualTo value
                commands.del(key)
            }
        }
    }
}
