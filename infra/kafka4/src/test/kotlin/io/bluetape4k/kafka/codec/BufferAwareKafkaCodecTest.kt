package io.bluetape4k.kafka.codec

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.apache.kafka.common.header.Headers
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BufferAwareKafkaCodecTest {
    private class RecordingCodec: BufferAwareKafkaCodec<String> {
        var serializeHeaders: Headers? = null
        var deserializeHeaders: Headers? = null

        override fun serialize(topic: String?, headers: Headers?, data: String?): ByteArray? =
            data?.encodeToByteArray()

        override fun deserialize(topic: String?, headers: Headers?, data: ByteArray?): String? =
            data?.decodeToString()

        override fun serializeTo(
            topic: String?,
            headers: Headers?,
            data: String,
            target: ByteBuffer,
        ): Int {
            serializeHeaders = headers
            val bytes = data.encodeToByteArray()
            target.put(bytes)
            return bytes.size
        }

        override fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): String {
            deserializeHeaders = headers
            return source.duplicate().let { view ->
                ByteArray(view.remaining()).also(view::get).decodeToString()
            }
        }
    }

    @Test
    fun `headerless output delegates with null headers`() {
        val codec = RecordingCodec()
        val target = ByteBuffer.allocate(16)

        codec.serializeTo("events", "hello", target) shouldBeEqualTo 5

        codec.serializeHeaders.shouldBeNull()
        target.position() shouldBeEqualTo 5
    }

    @Test
    fun `headerless input delegates with null headers`() {
        val codec = RecordingCodec()
        val source = ByteBuffer.wrap("hello".encodeToByteArray())

        codec.deserializeFrom("events", source) shouldBeEqualTo "hello"

        codec.deserializeHeaders.shouldBeNull()
        source.position() shouldBeEqualTo 0
    }
}
