package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class BinarySerializerDecoratorOutputStreamTest {

    @Test
    fun `stream serialization preserves Java style subclass transform`() {
        val serializer = JavaStyleDecorator(DirectStreamSerializer())
        val target = RecordingOutputStream()
        val expected = serializer.serialize("value")

        val written = serializer.serializeBinaryToStream("value", target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `stream serialization preserves Kotlin style subclass transform`() {
        val serializer = KotlinStyleDecorator(DirectStreamSerializer())
        val target = RecordingOutputStream()
        val expected = serializer.serialize("value")

        val written = serializer.serializeBinaryToStream("value", target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    private class JavaStyleDecorator(
        serializer: BinarySerializer,
    ): BinarySerializerDecorator(serializer) {
        override fun serialize(graph: Any?): ByteArray {
            val payload = super.serialize(graph).decodeToString()
            return "java-decorated:$payload".encodeToByteArray()
        }
    }

    private class KotlinStyleDecorator(
        serializer: BinarySerializer,
    ): BinarySerializerDecorator(serializer) {
        override fun serialize(graph: Any?): ByteArray =
            "kotlin-decorated:${super.serialize(graph).decodeToString()}".encodeToByteArray()
    }

    private class DirectStreamSerializer: BinarySerializer {
        override fun serialize(graph: Any?): ByteArray = graph.toString().encodeToByteArray()

        override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
            val bytes = "wrapped-direct".encodeToByteArray()
            target.write(bytes)
            return bytes.size
        }

        override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
    }

    private class RecordingOutputStream: OutputStream() {
        private val output = ByteArrayOutputStream()

        var flushCount: Int = 0
            private set

        var closeCount: Int = 0
            private set

        override fun write(value: Int) {
            output.write(value)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            output.write(bytes, offset, length)
        }

        override fun flush() {
            flushCount++
        }

        override fun close() {
            closeCount++
        }

        fun toByteArray(): ByteArray = output.toByteArray()
    }
}
