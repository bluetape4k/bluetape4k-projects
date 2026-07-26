package io.bluetape4k.protobuf.serializers

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

class ProtobufSerializerByteBufferTest {
    private val serializer = ProtobufSerializer()
    private val message = testMessage { id = 757L; name = "caller-buffer" }

    private data class FallbackValue(val id: Int): Serializable
    private class RecordingFallback: BinarySerializer {
        var serializeCalls = 0
        override fun serialize(graph: Any?): ByteArray = byteArrayOf(1, 2).also { serializeCalls++ }
        override fun <T: Any> deserialize(bytes: ByteArray?): T? = null
    }

    @Test
    fun `declares only the ByteBuffer encode optimization`() {
        ProtobufSerializer::class.java.getDeclaredMethod("serializeTo", Any::class.java, ByteBuffer::class.java)
    }

    @Test
    fun `protobuf wire bytes are identical for reusable heap and direct targets`() {
        val wire = serializer.serialize(message)
        listOf(ByteBuffer.allocate(wire.size + 8), ByteBuffer.allocateDirect(wire.size + 8)).forEach { target ->
            repeat(100) {
                target.clear().position(4)
                serializer.serializeTo(message, target) shouldBeEqualTo wire.size
                val actual = ByteArray(wire.size)
                target.duplicate().apply { position(4); limit(4 + wire.size) }.get(actual)
                actual.contentEquals(wire) shouldBeEqualTo true
            }
        }
    }

    @Test
    fun `deserialization preserves bounded heap direct sliced and read only source state`() {
        val wire = serializer.serialize(message)
        val heap = ByteBuffer.allocate(wire.size + 4).apply { position(2); put(wire); flip(); position(2) }
        val direct = ByteBuffer.allocateDirect(wire.size + 4).apply { position(2); put(wire); flip(); position(2) }
        val sliced =
            ByteBuffer.wrap(ByteArray(2) + wire + ByteArray(2)).apply { position(2); limit(2 + wire.size) }.slice()
        listOf(heap, direct, sliced, heap.asReadOnlyBuffer()).forEach { source ->
            source.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            val position = source.position();
            val limit = source.limit();
            val order = source.order()
            source.mark()
            serializer.deserializeFrom<TestMessage>(source) shouldBeEqualTo message
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
        }
        listOf(
            ByteBuffer.allocate(0),
            ByteBuffer.allocateDirect(0),
            ByteBuffer.allocate(2).apply { position(1); limit(1) }.slice(),
        ).forEach { serializer.deserializeFrom<TestMessage>(it).shouldBeNull() }
    }

    @Test
    fun `raw preflight failures and null preserve target state`() {
        val wire = serializer.serialize(message)
        val full = ByteBuffer.allocate(wire.size - 1).apply { position(1) }
        assertFailsWith<BufferOverflowException> { serializer.serializeTo(message, full) }
        full.position() shouldBeEqualTo 1
        val target = ByteBuffer.allocate(8).apply { position(3) }
        val limit = target.limit()
        serializer.serializeTo(null, target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 3
        target.limit() shouldBeEqualTo limit
    }

    @Test
    fun `read only target precedes protobuf null strict and trusted dispatch`() {
        val fallback = RecordingFallback()
        val trusted = ProtobufSerializer(fallback)
        listOf<Pair<ProtobufSerializer, Any?>>(
            serializer to message, serializer to null, serializer to "strict", trusted to FallbackValue(2),
        ).forEach { (subject, value) ->
            val target = ByteBuffer.allocate(64).asReadOnlyBuffer()
            assertFailsWith<ReadOnlyBufferException> { subject.serializeTo(value, target) }
            target.position() shouldBeEqualTo 0
        }
        fallback.serializeCalls shouldBeEqualTo 0
    }

    @Test
    fun `trusted fallback overflow calls fallback once and preserves target`() {
        val fallback = RecordingFallback()
        val target = ByteBuffer.allocate(1)
        assertFailsWith<BufferOverflowException> { ProtobufSerializer(fallback).serializeTo(FallbackValue(1), target) }
        fallback.serializeCalls shouldBeEqualTo 1
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `public constructor and factory retain trusted fallback parity`() {
        val value = FallbackValue(7)
        listOf(
            ProtobufSerializer(fallback = BinarySerializers.Kryo),
            ProtobufSerializer.trustedInternalProtobuf(BinarySerializers.Kryo),
        ).forEach { trusted ->
            val bytes = trusted.serialize(value)
            val target = ByteBuffer.allocate(bytes.size)
            trusted.serializeTo(value, target) shouldBeEqualTo bytes.size
            val actual = ByteArray(bytes.size).also { target.duplicate().apply { flip() }.get(it) }
            actual.contentEquals(bytes) shouldBeEqualTo true
            trusted.deserializeFrom<FallbackValue>(target.duplicate().apply { flip() }) shouldBeEqualTo value
        }
    }
}
