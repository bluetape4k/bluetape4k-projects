package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.serializer.BinarySerializers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class LettuceBinaryCodecSizeTest {

    private val codec = LettuceBinaryCodec<Any>(BinarySerializers.Jdk)

    @Test
    fun `estimateSize should handle string and byte array`() {
        codec.estimateSize("abc") shouldBeEqualTo 3
        codec.estimateSize(byteArrayOf(1, 2, 3)) shouldBeEqualTo 3
    }

    @Test
    fun `estimateSize should handle byte buffer remaining`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        buffer.get()
        codec.estimateSize(buffer) shouldBeEqualTo 3
    }

    @Test
    fun `estimateSize should fall back to serializer for custom object`() {
        val value = SampleValue(1, "name")
        codec.estimateSize(value) shouldBeGreaterThan 0
    }

    data class SampleValue(
        val id: Int,
        val name: String,
    ): java.io.Serializable
}
