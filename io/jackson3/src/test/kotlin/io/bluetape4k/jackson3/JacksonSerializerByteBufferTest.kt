package io.bluetape4k.jackson3

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize as deserializeRaw
import org.junit.jupiter.api.Test
import tools.jackson.databind.SerializationFeature
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class JacksonSerializerByteBufferTest {

    @Test
    fun `ByteBuffer paths bypass allocating ByteArray overrides`() {
        val baseline = JacksonSerializer()
        val expected = CollectionItem(7, "buffer-user")
        val wire = baseline.serialize(expected)
        val serializer = NoByteArrayJacksonSerializer()
        val target = ByteBuffer.allocate(wire.size)

        serializer.serializeTo(expected, target) shouldBeEqualTo wire.size
        target.flip()
        serializer.deserializeFrom(target, CollectionItem::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `ByteArray and ByteBuffer paths remain wire compatible`() {
        val serializer = JacksonSerializer()
        val expected = CollectionItem(8, "wire-compatible")

        serializer.deserializeFrom(
            ByteBuffer.wrap(serializer.serialize(expected)),
            CollectionItem::class.java,
        ) shouldBeEqualTo expected

        val target = ByteBuffer.allocate(256)
        serializer.serializeTo(expected, target)
        val wire = target.writtenBytes(0, target.position())
        serializer.deserialize(wire, CollectionItem::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `concrete ByteBuffer overload preserves parameterized collection types`() {
        val serializer = JacksonSerializer()
        val users = listOf(CollectionItem(1, "alpha"), CollectionItem(2, "beta"))
        val usersByName = users.associateBy { it.name }

        serializer.deserialize<List<CollectionItem>>(ByteBuffer.wrap(serializer.serialize(users))) shouldBeEqualTo users
        serializer.deserialize<Map<String, CollectionItem>>(ByteBuffer.wrap(serializer.serialize(usersByName))) shouldBeEqualTo usersByName
    }

    @Test
    fun `legacy subclass may retain the ByteBuffer deserialize JVM signature`() {
        val legacy = LegacySignatureJacksonSerializer()
        legacy.deserialize<CollectionItem>(ByteBuffer.allocate(0)).shouldBeNull()

        val serializer: JacksonSerializer = legacy
        val expected = CollectionItem(3, "extension-dispatch")
        serializer.deserialize<CollectionItem>(ByteBuffer.wrap(serializer.serialize(expected))) shouldBeEqualTo expected
    }

    @Test
    fun `heap direct sliced and read-only sources preserve caller state`() {
        val serializer = JacksonSerializer()
        val expected = CollectionItem(9, "stateful")
        val wire = serializer.serialize(expected)

        sourceVariants(wire).forEach { source ->
            val position = source.position()
            val limit = source.limit()
            val order = source.order()
            source.mark()

            serializer.deserializeFrom(source, CollectionItem::class.java) shouldBeEqualTo expected
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo position
        }
    }

    @Test
    fun `bounded output commits only the written range`() {
        val serializer = JacksonSerializer()
        val value = CollectionItem(11, "bounded")
        val wire = serializer.serialize(value)
        val target = ByteBuffer.allocate(wire.size + 6).order(ByteOrder.LITTLE_ENDIAN)
        target.position(3)
        target.limit(3 + wire.size)

        serializer.serializeTo(value, target) shouldBeEqualTo wire.size
        target.position() shouldBeEqualTo 3 + wire.size
        target.limit() shouldBeEqualTo 3 + wire.size
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        target.writtenBytes(3, wire.size).contentEquals(wire).shouldBeTrue()
    }

    @Test
    fun `read-only null target is rejected before mapper work`() {
        val serializer = NoByteArrayJacksonSerializer()
        val target = ByteBuffer.allocate(8).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> {
            serializer.serializeTo(null, target)
        }
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `null output is a no-op and overflow rolls back for retry`() {
        val serializer = JacksonSerializer()
        val value = CollectionItem(12, "retry")
        val wire = serializer.serialize(value)
        val target = ByteBuffer.allocate(wire.size + 2)
        target.position(2)
        target.limit(2)

        serializer.serializeTo(null, target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 2

        target.limit(target.capacity() - 1)
        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo(value, target)
        }
        target.position() shouldBeEqualTo 2

        target.limit(target.capacity())
        serializer.serializeTo(value, target) shouldBeEqualTo wire.size
    }

    @Test
    fun `malformed and empty input keep wrapper state and serializer remains reusable`() {
        val serializer = JacksonSerializer()
        val malformed = ByteBuffer.wrap("{not-json".toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
        malformed.position(1)
        malformed.mark()
        val position = malformed.position()
        val limit = malformed.limit()

        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(malformed, CollectionItem::class.java)
        }
        malformed.position() shouldBeEqualTo position
        malformed.limit() shouldBeEqualTo limit
        malformed.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        malformed.reset()

        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(ByteBuffer.allocate(0), CollectionItem::class.java)
        }

        val expected = CollectionItem(13, "reused")
        serializer.deserializeFrom(ByteBuffer.wrap(serializer.serialize(expected)), CollectionItem::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `fatal serialization error keeps identity and output position`() {
        val serializer = JacksonSerializer()
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
        val serializer = JacksonSerializer()
        val fatal = SerializerFatalError()
        fatalReadFailure = fatal

        try {
            val classTokenFailure = assertFailsWith<SerializerFatalError> {
                serializer.deserializeFrom(
                    ByteBuffer.wrap("""{"value":"boom"}""".toByteArray()),
                    FatalReadGraph::class.java,
                )
            }
            (classTokenFailure === fatal).shouldBeTrue()

            val reifiedFailure = assertFailsWith<SerializerFatalError> {
                serializer.deserialize<FatalReadGraph>(ByteBuffer.wrap("""{"value":"boom"}""".toByteArray()))
            }
            (reifiedFailure === fatal).shouldBeTrue()
        } finally {
            fatalReadFailure = null
        }
    }

    @Test
    fun `suppressed overflow does not replace the primary serialization failure`() {
        val serializer = JacksonSerializer()
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
    fun `configured mapper applies and unsolicited type metadata stays data`() {
        val indentedMapper = Jackson.createDefaultJsonMapper()
            .rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
        val serializer = JacksonSerializer(indentedMapper)
        val value = CollectionItem(14, "configured")
        val target = ByteBuffer.allocate(256)
        serializer.serializeTo(value, target)
        target.writtenBytes(0, target.position()).decodeToString() shouldContain "\n"

        val metadata = """{"@class":"java.lang.ProcessBuilder","value":"blocked"}"""
        val restored = serializer.deserializeFrom(ByteBuffer.wrap(metadata.toByteArray()), Any::class.java)
        restored.shouldBeInstanceOf<Map<*, *>>()["@class"] shouldBeEqualTo "java.lang.ProcessBuilder"
    }

    @Test
    fun `annotation driven polymorphism is preserved on ByteBuffer paths`() {
        val serializer = JacksonSerializer()
        val expected: Person = Professor("buffer-professor", 42, "coroutines")
        val target = ByteBuffer.allocate(512)

        serializer.serializeTo(expected, target)
        target.flip()

        serializer.deserializeFrom(target, Person::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `annotation driven polymorphism rejects an unrelated class id`() {
        val serializer = JacksonSerializer()
        val payload = """{"@class":"java.lang.ProcessBuilder","name":"blocked","age":1}"""

        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(ByteBuffer.wrap(payload.toByteArray()), Person::class.java)
        }
    }

    @Test
    fun `interface receiver retains raw collection behavior and invalid class is wrapped`() {
        val concrete = JacksonSerializer()
        val serializer: JsonSerializer = concrete
        val values = listOf(CollectionItem(1, "raw"))

        val restored: Any? = serializer.deserializeRaw<List<CollectionItem>>(ByteBuffer.wrap(concrete.serialize(values)))
        (restored as List<*>).first().shouldBeInstanceOf<Map<*, *>>()

        assertFailsWith<JsonSerializationException> {
            serializer.deserializeRaw(ByteBuffer.wrap(concrete.serialize(values)), CollectionItem::class.java)
        }
    }
}

private class NoByteArrayJacksonSerializer: JacksonSerializer() {
    override fun serialize(graph: Any?): ByteArray = error("ByteArray serialization fallback must not run")

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        error("ByteArray deserialization fallback must not run")
}

private class LegacySignatureJacksonSerializer: JacksonSerializer() {
    fun <T: Any> deserialize(source: ByteBuffer): T? {
        source.remaining()
        return null
    }
}

private class FatalGraph(private val failure: Error) {
    val value: String
        get() = throw failure
}

private class FailureGraph(private val failure: RuntimeException) {
    val value: String
        get() = throw failure
}

private class SerializerFatalError: Error()

private var fatalReadFailure: Error? = null

private class FatalReadGraph {
    var value: String? = null
        set(value) {
            field = value
            fatalReadFailure?.let { throw it }
        }
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
