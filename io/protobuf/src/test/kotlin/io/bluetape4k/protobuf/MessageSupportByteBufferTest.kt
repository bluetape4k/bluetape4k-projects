package io.bluetape4k.protobuf

import com.google.protobuf.Message
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.testMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class MessageSupportByteBufferTest {
    @Test
    fun `packMessageTo rejects read-only target before protobuf work`() {
        val message = mockk<Message>()
        every { message.descriptorForType } throws AssertionError("protobuf work must not run")
        val target = ByteBuffer.allocate(1).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> { packMessageTo(message, target) }

        verify(exactly = 0) { message.descriptorForType }
    }

    @Test
    fun `packMessageTo writes Any wire bytes to heap and direct buffers from nonzero positions`() {
        val message = message()
        val expected = packMessage(message)

        listOf(ByteBuffer.allocate(expected.size + 8), ByteBuffer.allocateDirect(expected.size + 8)).forEach { target ->
            target.order(ByteOrder.LITTLE_ENDIAN)
            target.position(3)
            target.limit(3 + expected.size)
            val limit = target.limit()
            val capacity = target.capacity()
            val order = target.order()

            assertEquals(expected.size, packMessageTo(message, target))
            assertEquals(3 + expected.size, target.position())
            assertEquals(limit, target.limit())
            assertEquals(capacity, target.capacity())
            assertEquals(order, target.order())
            assertContentEquals(expected, target.bytesAt(3, expected.size))
        }
    }

    @Test
    fun `packMessageTo fills exact heap and direct capacity`() {
        val message = message()
        val size = packMessage(message).size

        listOf(ByteBuffer.allocate(size), ByteBuffer.allocateDirect(size)).forEach { target ->
            assertEquals(size, packMessageTo(message, target))
            assertEquals(target.limit(), target.position())
        }
    }

    @Test
    fun `unpackMessage returns null for empty heap direct and sliced sources without state drift`() {
        val sliced = ByteBuffer.allocate(8).apply {
            position(4)
            limit(4)
        }.slice()

        listOf(ByteBuffer.allocate(0), ByteBuffer.allocateDirect(0), sliced).forEach { source ->
            source.mark()
            val state = source.state()

            assertNull(unpackMessage<TestMessage>(source))
            source.assertState(state)
        }
    }

    @Test
    fun `packMessageTo preserves target state and content for read-only and undersized preflight failures`() {
        val message = message()
        val size = packMessage(message).size
        val writable = ByteBuffer.allocate(size + 2).apply { put(ByteArray(capacity()) { 0x3C }) }
        writable.position(1)
        writable.limit(size + 1)
        val readOnly = writable.asReadOnlyBuffer()
        readOnly.mark()
        val readOnlyState = readOnly.state()
        val readOnlyContent = writable.array().copyOf()

        assertFailsWith<ReadOnlyBufferException> { packMessageTo(message, readOnly) }
        readOnly.assertState(readOnlyState)
        assertContentEquals(readOnlyContent, writable.array())

        val undersized = ByteBuffer.allocate(size + 2).apply { put(ByteArray(capacity()) { 0x5A }) }
        undersized.position(1)
        undersized.limit(size)
        undersized.mark()
        val undersizedState = undersized.state()
        val undersizedContent = undersized.array().copyOf()

        assertFailsWith<BufferOverflowException> { packMessageTo(message, undersized) }
        undersized.assertState(undersizedState)
        assertContentEquals(undersizedContent, undersized.array())
    }

    @Test
    fun `unpackMessage reads bounded read-only direct source without changing its state`() {
        val encoded = packMessage(message())
        val source = ByteBuffer.allocateDirect(encoded.size + 8).apply {
            position(3)
            put(encoded)
            limit(3 + encoded.size)
            position(3)
        }.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        source.mark()
        val state = source.state()

        val decoded = unpackMessage<TestMessage>(source)

        assertEquals(message(), decoded)
        source.assertState(state)
    }

    @Test
    fun `unpackMessage preserves malformed source state`() {
        val source = ByteBuffer.wrap(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte())).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            position(1)
            mark()
        }
        val state = source.state()

        assertFailsWith<Exception> { unpackMessage<TestMessage>(source) }
        source.assertState(state)
    }

    @Test
    fun `writePackedAnyTo restores position but not written content after post-write failure`() {
        val packed = ProtoAny.pack(message())
        val target = ByteBuffer.allocate(packed.serializedSize + 2).apply { position(1) }
        val position = target.position()

        assertFailsWith<IllegalStateException> {
            writePackedAnyTo(packed, target) { _: ProtoAny, buffer: ByteBuffer ->
                buffer.put(0x7F)
                throw IllegalStateException("injected post-write failure")
            }
        }

        assertEquals(position, target.position())
        assertEquals(0x7F, target.get(position).toInt() and 0xFF)
    }

    private fun message(): TestMessage = testMessage {
        id = 757L
        name = "bounded buffer"
    }

    private fun ByteBuffer.bytesAt(position: Int, size: Int): ByteArray =
        duplicate().apply {
            position(position)
            limit(position + size)
        }.let { duplicate -> ByteArray(size).also(duplicate::get) }

    private fun ByteBuffer.state(): BufferState = BufferState(position(), limit(), order())

    private fun ByteBuffer.assertState(expected: BufferState) {
        assertEquals(expected.position, position())
        assertEquals(expected.limit, limit())
        assertEquals(expected.order, order())
        reset()
        assertEquals(expected.position, position())
    }

    private data class BufferState(val position: Int, val limit: Int, val order: ByteOrder)
}
