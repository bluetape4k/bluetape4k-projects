package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.io.compressor.Compressors
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException

class CompressableBinarySerializerOutputStreamTest {

    @Test
    fun `stream serialization preserves compressed wire ahead of wrapped direct stream`() {
        val serializer = CompressableBinarySerializer(DirectStreamSerializer(), Compressors.LZ4)
        val target = RecordingOutputStream()
        val expected = serializer.serialize("compressible-value".repeat(64))

        val written = serializer.serializeBinaryToStream("compressible-value".repeat(64), target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `stream serialization restores nested cancellation identity`() {
        val cancellation = CancellationException("cancelled")

        verifyNestedControlFailure(cancellation)
    }

    @Test
    fun `stream serialization restores nested fatal failure identity`() {
        val fatal = AssertionError("fatal")

        verifyNestedControlFailure(fatal)
    }

    private fun verifyNestedControlFailure(failure: Throwable) {
        val serializer = CompressableBinarySerializer(WrappingSerializer(failure), Compressors.LZ4)
        val target = RecordingOutputStream()

        val actual = assertFailsWith<Throwable> {
            serializer.serializeBinaryToStream("value", target)
        }

        actual shouldBeSameInstanceAs failure
        target.toByteArray() shouldBeEqualTo byteArrayOf()
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    private class DirectStreamSerializer: BinarySerializer {
        override fun serialize(graph: Any?): ByteArray = graph.toString().encodeToByteArray()

        override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
            val bytes = "uncompressed-wrapped-direct".encodeToByteArray()
            target.write(bytes)
            return bytes.size
        }

        override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
    }

    private class WrappingSerializer(
        private val failure: Throwable,
    ): BinarySerializer {
        override fun serialize(graph: Any?): ByteArray =
            throw BinarySerializationException("wrapped", failure)

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
