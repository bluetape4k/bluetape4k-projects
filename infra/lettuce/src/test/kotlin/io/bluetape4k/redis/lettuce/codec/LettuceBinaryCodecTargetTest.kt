package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.JdkBinarySerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import java.io.Serializable

class LettuceBinaryCodecTargetTest {

    private data class SampleValue(val id: Int): Serializable

    private class RecordingSerializer: BinarySerializer {
        var serializeCalls = 0

        override fun serialize(graph: Any?): ByteArray =
            byteArrayOf(1, 2, 3).also { serializeCalls++ }

        override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
    }

    private class RecordingCodec(
        serializer: BinarySerializer,
    ): LettuceBinaryCodec<SampleValue>(serializer) {
        var encodeCalls = 0

        override fun encodeValue(value: SampleValue, target: ByteBuf?) {
            encodeCalls++
            target?.writeInt(value.id)
        }
    }

    @Test
    fun `one argument constructor retains serializer estimate and description contracts`() {
        val serializer = JdkBinarySerializer()
        val codec = LettuceBinaryCodec<SampleValue>(serializer)

        codec.serializer shouldBeSameInstanceAs serializer
        codec.estimateSize("key") shouldBeEqualTo 3
        codec.estimateSize(SampleValue(1)) shouldBeEqualTo -1
        codec.toString() shouldBeEqualTo "LettuceBinaryCodec(serializer=JdkBinarySerializer)"
    }

    @Test
    fun `null target is a no-op before serialization`() {
        val serializer = RecordingSerializer()
        val codec = LettuceBinaryCodec<SampleValue>(serializer)

        codec.encodeValue(SampleValue(1), null)

        serializer.serializeCalls shouldBeEqualTo 0
    }

    @Test
    fun `target encoding preserves prefix and appends serialized bytes`() {
        val serializer = JdkBinarySerializer()
        val codec = LettuceBinaryCodec<SampleValue>(serializer)
        val value = SampleValue(757)
        val wire = serializer.serialize(value)
        val prefix = byteArrayOf(9, 8, 7)
        val target = Unpooled.buffer(prefix.size + wire.size)

        try {
            target.writeBytes(prefix)

            codec.encodeValue(value, target)

            val actual = ByteArray(prefix.size + wire.size)
            target.getBytes(0, actual)
            actual.contentEquals(prefix + wire).shouldBeTrue()
            target.writerIndex() shouldBeEqualTo actual.size
        } finally {
            target.release()
        }
    }

    @Test
    fun `subclass can specialize only target encoding`() {
        val codec = RecordingCodec(JdkBinarySerializer())
        val target = Unpooled.buffer(Int.SIZE_BYTES)

        try {
            codec.encodeValue(SampleValue(757), target)

            codec.encodeCalls shouldBeEqualTo 1
            target.writerIndex() shouldBeEqualTo Int.SIZE_BYTES
            target.getInt(0) shouldBeEqualTo 757
        } finally {
            target.release()
        }
    }
}
