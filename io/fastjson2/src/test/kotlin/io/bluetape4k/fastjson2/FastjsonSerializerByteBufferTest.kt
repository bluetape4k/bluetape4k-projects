package io.bluetape4k.fastjson2

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.JSONFactory
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reader.ObjectReader
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class FastjsonSerializerByteBufferTest {

    @Test
    fun `concrete ByteBuffer overload preserves parameterized collection types`() {
        val serializer = FastjsonSerializer()
        val users = listOf(User(1, "alpha"), User(2, "beta"))
        val usersByName = users.associateBy { it.name }

        serializer.deserialize<List<User>>(ByteBuffer.wrap(serializer.serialize(users))) shouldBeEqualTo users
        serializer.deserialize<Map<String, User>>(ByteBuffer.wrap(serializer.serialize(usersByName))) shouldBeEqualTo usersByName
    }

    @Test
    fun `old and new JSONB paths cross read`() {
        val serializer = FastjsonSerializer()
        val expected = User(7, "cross-read")
        val target = ByteBuffer.allocate(256)

        val written = serializer.serializeTo(expected, target)
        val newWire = target.writtenBytes(0, written)
        serializer.deserialize(newWire, User::class.java) shouldBeEqualTo expected
        serializer.deserializeFrom(ByteBuffer.wrap(serializer.serialize(expected)), User::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `heap direct sliced and read-only sources preserve caller state`() {
        val serializer = FastjsonSerializer()
        val expected = User(8, "stateful")
        val wire = serializer.serialize(expected)

        sourceVariants(wire).forEach { source ->
            val position = source.position()
            val limit = source.limit()
            val order = source.order()
            source.mark()

            serializer.deserializeFrom(source, User::class.java) shouldBeEqualTo expected
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo position
        }
    }

    @Test
    fun `bounded compatibility output commits exact JSONB range`() {
        val serializer = FastjsonSerializer()
        val expected = User(9, "bounded")
        val wire = serializer.serialize(expected)
        val target = ByteBuffer.allocate(wire.size + 6).order(ByteOrder.LITTLE_ENDIAN)
        target.position(3)
        target.limit(3 + wire.size)

        serializer.serializeTo(expected, target) shouldBeEqualTo wire.size
        target.position() shouldBeEqualTo 3 + wire.size
        target.limit() shouldBeEqualTo 3 + wire.size
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        target.writtenBytes(3, wire.size).contentEquals(wire).shouldBeTrue()
    }

    @Test
    fun `read-only target rejects null and null output is otherwise a no-op`() {
        val serializer = FastjsonSerializer()
        val readOnly = ByteBuffer.allocate(8).asReadOnlyBuffer()
        assertFailsWith<ReadOnlyBufferException> {
            serializer.serializeTo(null, readOnly)
        }

        val target = ByteBuffer.allocate(8)
        target.position(4)
        serializer.serializeTo(null, target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 4
    }

    @Test
    fun `overflow rolls back and target is reusable`() {
        val serializer = FastjsonSerializer()
        val expected = User(10, "retry")
        val wire = serializer.serialize(expected)
        val target = ByteBuffer.allocate(wire.size + 2)
        target.position(2)
        target.limit(target.capacity() - 1)

        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo(expected, target)
        }
        target.position() shouldBeEqualTo 2

        target.limit(target.capacity())
        serializer.serializeTo(expected, target) shouldBeEqualTo wire.size
    }

    @Test
    fun `malformed and empty input preserve state and serializer remains reusable`() {
        val serializer = FastjsonSerializer()
        val malformed = ByteBuffer.wrap(byteArrayOf(0x7F, 0x00, 0x01)).order(ByteOrder.LITTLE_ENDIAN)
        malformed.mark()

        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(malformed, User::class.java)
        }
        malformed.position() shouldBeEqualTo 0
        malformed.limit() shouldBeEqualTo 3
        malformed.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        malformed.reset()
        serializer.deserializeFrom(ByteBuffer.allocate(0), User::class.java).shouldBeNull()

        val expected = User(11, "reused")
        serializer.deserializeFrom(ByteBuffer.wrap(serializer.serialize(expected)), User::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `fatal serialization error keeps identity and output position`() {
        val serializer = FastjsonSerializer()
        val fatal = SerializerFatalError()
        val target = ByteBuffer.allocate(64)
        target.position(5)

        val thrown = assertFailsWith<SerializerFatalError> {
            serializer.serializeTo(FatalGraph(fatal), target)
        }

        (thrown === fatal).shouldBeTrue()
        target.position() shouldBeEqualTo 5
    }

    @Test
    fun `fatal deserialization error keeps identity for class token and reified paths`() {
        val serializer = FastjsonSerializer()
        val fatal = SerializerFatalError()
        val payload = serializer.serialize(emptyMap<String, String>())
        val provider = JSONFactory.getDefaultObjectReaderProvider()
        val reader = throwingObjectReader<FatalReadGraph>(IllegalStateException("wrapper", fatal))
        val previous = provider.register(FatalReadGraph::class.java, reader)

        try {
            val classTokenFailure = assertFailsWith<SerializerFatalError> {
                serializer.deserializeFrom(ByteBuffer.wrap(payload), FatalReadGraph::class.java)
            }
            (classTokenFailure === fatal).shouldBeTrue()

            val reifiedFailure = assertFailsWith<SerializerFatalError> {
                serializer.deserialize<FatalReadGraph>(ByteBuffer.wrap(payload))
            }
            (reifiedFailure === fatal).shouldBeTrue()
        } finally {
            if (previous == null) {
                provider.unregisterObjectReader(FatalReadGraph::class.java)
            } else {
                provider.register(FatalReadGraph::class.java, previous)
            }
        }
    }

    @Test
    fun `suppressed fatal error does not replace the primary deserialization failure`() {
        val serializer = FastjsonSerializer()
        val fatal = SerializerFatalError()
        val primary = IllegalStateException("primary").apply { addSuppressed(fatal) }
        val payload = serializer.serialize(emptyMap<String, String>())
        val provider = JSONFactory.getDefaultObjectReaderProvider()
        val reader = throwingObjectReader<SuppressedFailureReadGraph>(primary)
        val previous = provider.register(SuppressedFailureReadGraph::class.java, reader)

        try {
            val classTokenFailure = assertFailsWith<JsonSerializationException> {
                serializer.deserializeFrom(ByteBuffer.wrap(payload), SuppressedFailureReadGraph::class.java)
            }
            (classTokenFailure.cause === primary).shouldBeTrue()

            val reifiedFailure = assertFailsWith<JsonSerializationException> {
                serializer.deserialize<SuppressedFailureReadGraph>(ByteBuffer.wrap(payload))
            }
            (reifiedFailure.cause === primary).shouldBeTrue()
        } finally {
            if (previous == null) {
                provider.unregisterObjectReader(SuppressedFailureReadGraph::class.java)
            } else {
                provider.register(SuppressedFailureReadGraph::class.java, previous)
            }
        }
    }

    @Test
    fun `suppressed overflow does not replace the primary serialization failure`() {
        val serializer = FastjsonSerializer()
        val primary = IllegalStateException("primary").apply {
            addSuppressed(BufferOverflowException())
        }
        val target = ByteBuffer.allocate(64)
        target.position(4)

        assertFailsWith<JsonSerializationException> {
            serializer.serializeTo(FailureGraph(primary), target)
        }
        target.position() shouldBeEqualTo 4
    }

    @Test
    fun `interface receiver stays raw and invalid class is wrapped`() {
        val concrete = FastjsonSerializer()
        val serializer: JsonSerializer = concrete
        val users = listOf(User(1, "raw"))

        val restored: Any? = serializer.deserialize<List<User>>(ByteBuffer.wrap(concrete.serialize(users)))
        (restored as List<*>).first().shouldBeInstanceOf<Map<*, *>>()

        assertFailsWith<JsonSerializationException> {
            serializer.deserialize(ByteBuffer.wrap(concrete.serialize(users)), User::class.java)
        }
    }

    @Test
    fun `type metadata stays data without AutoType`() {
        val serializer = FastjsonSerializer()
        val metadata = mapOf("@type" to "java.lang.ProcessBuilder", "value" to "blocked")
        val restored = serializer.deserializeFrom(ByteBuffer.wrap(serializer.serialize(metadata)), Any::class.java)

        restored.shouldBeInstanceOf<Map<*, *>>()["@type"] shouldBeEqualTo "java.lang.ProcessBuilder"
    }

    @Test
    fun `JSONB typed any metadata stays data without AutoType`() {
        val serializer = FastjsonSerializer()
        val payload = JSONB.toBytes(AutoTypePayload("blocked"), JSONWriter.Feature.WriteClassName)
        val heap = ByteBuffer.wrap(payload)
        val direct = ByteBuffer.allocateDirect(payload.size).apply {
            put(payload)
            flip()
        }

        listOf(heap, direct).forEach { source ->
            serializer.deserializeFrom(source, Any::class.java)
                .shouldBeInstanceOf<Map<*, *>>()["value"] shouldBeEqualTo "blocked"
            serializer.deserialize<Any>(source)
                .shouldBeInstanceOf<Map<*, *>>()["value"] shouldBeEqualTo "blocked"
        }
    }
}

private data class AutoTypePayload(
    val value: String,
)

private class FatalGraph(private val failure: Error) {
    val value: String
        get() = throw failure
}

private class FailureGraph(private val failure: RuntimeException) {
    val value: String
        get() = throw failure
}

private class SerializerFatalError: Error()

private class FatalReadGraph

private class SuppressedFailureReadGraph

private fun <T: Any> throwingObjectReader(failure: Throwable): ObjectReader<T> = object: ObjectReader<T> {
    override fun readObject(
        jsonReader: JSONReader,
        fieldType: Type?,
        fieldName: Any?,
        features: Long,
    ): T = throw failure

    override fun readJSONBObject(
        jsonReader: JSONReader,
        fieldType: Type?,
        fieldName: Any?,
        features: Long,
    ): T = throw failure
}

private fun sourceVariants(bytes: ByteArray): List<ByteBuffer> {
    fun filled(buffer: ByteBuffer): ByteBuffer = buffer.apply {
        position(2)
        put(bytes)
        limit(2 + bytes.size)
        position(2)
        order(ByteOrder.LITTLE_ENDIAN)
    }

    val heap = filled(ByteBuffer.allocate(bytes.size + 4))
    val direct = filled(ByteBuffer.allocateDirect(bytes.size + 4))
    val parent = ByteBuffer.allocate(bytes.size + 8).apply {
        position(3)
        put(bytes)
        limit(3 + bytes.size)
        position(3)
    }
    val sliced = parent.slice().apply {
        limit(bytes.size)
        order(ByteOrder.LITTLE_ENDIAN)
    }
    val readOnly = filled(ByteBuffer.allocate(bytes.size + 4)).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    return listOf(heap, direct, sliced, readOnly)
}

private fun ByteBuffer.writtenBytes(start: Int, size: Int): ByteArray =
    ByteArray(size).also { bytes ->
        duplicate().apply {
            position(start)
            limit(start + size)
            get(bytes)
        }
    }
