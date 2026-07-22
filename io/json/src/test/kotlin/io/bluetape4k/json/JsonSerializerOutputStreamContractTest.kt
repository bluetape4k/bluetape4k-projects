package io.bluetape4k.json

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class JsonSerializerOutputStreamContractTest {

    @Test
    fun `default stream serialization preserves ByteArray parity and reports the written count`() {
        val serializer = jsonSerializer()
        val target = RecordingOutputStream()
        val expected = serializer.serialize("json-value")

        val written = serializer.serializeJsonToStream("json-value", target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo target.toByteArray().size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `null input delegates to the backend serializer policy`() {
        var serializeCount = 0
        val serializer = jsonSerializer(serialize = { graph ->
            serializeCount++
            if (graph == null) JSON_NULL_PAYLOAD else JSON_PAYLOAD
        })
        val target = RecordingOutputStream()

        val written = serializer.serializeJsonToStream(null, target)

        serializeCount shouldBeEqualTo 1
        target.toByteArray() shouldBeEqualTo JSON_NULL_PAYLOAD
        written shouldBeEqualTo JSON_NULL_PAYLOAD.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `non-null zero-byte result writes nothing and returns zero`() {
        var serializeCount = 0
        val serializer = jsonSerializer(serialize = {
            serializeCount++
            byteArrayOf()
        })
        val target = RecordingOutputStream()

        val written = serializer.serializeJsonToStream("empty-json-value", target)

        serializeCount shouldBeEqualTo 1
        target.toByteArray() shouldBeEqualTo byteArrayOf()
        written shouldBeEqualTo 0
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `serializer failures retain identity and cause without touching the target`() {
        val backendCause = IllegalArgumentException("backend cause")
        val serializerFailure = IllegalStateException("serializer failure", backendCause)
        val serializer = jsonSerializer(serialize = { throw serializerFailure })
        val target = RecordingOutputStream()

        val actual = assertFailsWith<IllegalStateException> {
            serializer.serializeJsonToStream("json-value", target)
        }

        actual shouldBeSameInstanceAs serializerFailure
        actual.cause shouldBeSameInstanceAs backendCause
        target.toByteArray() shouldBeEqualTo byteArrayOf()
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `target write failures retain identity and may leave partial output`() {
        val writeFailure = IOException("target failure")
        val serializer = jsonSerializer()
        val target = RecordingOutputStream(writeFailure = writeFailure, bytesBeforeFailure = 1)

        val actual = assertFailsWith<IOException> {
            serializer.serializeJsonToStream("json-value", target)
        }

        actual shouldBeSameInstanceAs writeFailure
        target.toByteArray() shouldBeEqualTo JSON_PAYLOAD.copyOf(1)
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `repeated calls on one serializer retain no prior stream state`() {
        val serializer = jsonSerializer(serialize = { graph -> graph.toString().encodeToByteArray() })
        val firstTarget = RecordingOutputStream()
        val secondTarget = RecordingOutputStream()

        val firstWritten = serializer.serializeJsonToStream("first", firstTarget)
        val secondWritten = serializer.serializeJsonToStream("second", secondTarget)

        firstTarget.toByteArray() shouldBeEqualTo "first".encodeToByteArray()
        firstWritten shouldBeEqualTo firstTarget.toByteArray().size
        secondTarget.toByteArray() shouldBeEqualTo "second".encodeToByteArray()
        secondWritten shouldBeEqualTo secondTarget.toByteArray().size
        firstTarget.flushCount shouldBeEqualTo 0
        firstTarget.closeCount shouldBeEqualTo 0
        secondTarget.flushCount shouldBeEqualTo 0
        secondTarget.closeCount shouldBeEqualTo 0
    }

    private fun jsonSerializer(
        serialize: (Any?) -> ByteArray = { graph -> if (graph == null) byteArrayOf() else JSON_PAYLOAD },
    ): JsonSerializer =
        object: JsonSerializer {
            override fun serialize(graph: Any?): ByteArray = serialize.invoke(graph)

            override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? = null
        }

    private class RecordingOutputStream(
        private val writeFailure: IOException? = null,
        private val bytesBeforeFailure: Int = 0,
    ): OutputStream() {
        private val output = ByteArrayOutputStream()

        var flushCount: Int = 0
            private set

        var closeCount: Int = 0
            private set

        override fun write(value: Int) {
            writeFailure?.let { throw it }
            output.write(value)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            writeFailure?.let { failure ->
                output.write(bytes, offset, minOf(bytesBeforeFailure, length))
                throw failure
            }
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

    private companion object {
        val JSON_PAYLOAD: ByteArray = byteArrayOf(2, 4, 6, 8)
        val JSON_NULL_PAYLOAD: ByteArray = "null".encodeToByteArray()
    }
}
