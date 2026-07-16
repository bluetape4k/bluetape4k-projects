package io.bluetape4k.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.lang.reflect.InvocationTargetException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class FixedByteBufferOutputStreamTest {

    @TestFactory
    fun `fixed stream writes through the exact supplied buffer view`(): List<DynamicTest> =
        listOf(
            "heap" to ByteBuffer.allocate(16),
            "direct" to ByteBuffer.allocateDirect(16),
            "slice" to ByteBuffer.allocate(24).apply {
                position(4)
                limit(20)
            }.slice(),
        ).map { (name, target) ->
            DynamicTest.dynamicTest(name) {
                target.order(ByteOrder.LITTLE_ENDIAN)
                target.position(3)
                target.limit(12)
                val start = target.position()
                target.mark()
                val originalCapacity = target.capacity()
                val originalLimit = target.limit()
                val originalOrder = target.order()
                val payload = byteArrayOf(10, 20, 30, 40)

                val stream = ByteBufferOutputStream.fixed(target)
                stream.write(payload)
                stream.close()

                assertEquals(start + payload.size, target.position())
                assertEquals(originalCapacity, target.capacity())
                assertEquals(originalLimit, target.limit())
                assertEquals(originalOrder, target.order())
                assertArrayEquals(payload, target.bytesBetween(start, start + payload.size))

                val positionBeforeCopy = target.position()
                target.reset()
                assertEquals(start, target.position(), "write must preserve a mark below the committed position")
                target.position(positionBeforeCopy)
                assertArrayEquals(payload, stream.toByteArray())
                assertEquals(positionBeforeCopy, target.position())

                stream.write(50)
                assertEquals(positionBeforeCopy + 1, target.position(), "close must not disable later writes")
                assertArrayEquals(byteArrayOf(10, 20, 30, 40, 50), stream.toByteArray())
            }
        }

    @Test
    fun `fixed stream accepts exact remaining capacity`() {
        val target = ByteBuffer.allocate(8).apply {
            put(0, 99)
            position(2)
            limit(6)
        }
        val stream = ByteBufferOutputStream.fixed(target)

        stream.write(byteArrayOf(1, 2, 3, 4))

        assertEquals(6, target.position())
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), stream.toByteArray())
        assertEquals(99, target.get(0))
    }

    @Test
    fun `fixed stream throws raw overflow without growth or position movement`() {
        val target = ByteBuffer.allocate(10).apply {
            put(0, 91)
            put(6, 92)
            position(2)
            limit(5)
        }
        val stream = ByteBufferOutputStream.fixed(target)
        val start = target.position()

        val failure = assertThrows(BufferOverflowException::class.java) {
            stream.write(byteArrayOf(1, 2, 3, 4))
        }

        assertEquals(BufferOverflowException::class.java, failure.javaClass)
        assertEquals(start, target.position())
        assertEquals(91, target.get(0), "prefix canary")
        assertEquals(92, target.duplicate().limit(target.capacity()).get(6), "post-limit canary")
        assertArrayEquals(byteArrayOf(), stream.toByteArray())
    }

    @Test
    fun `fixed factory rejects read-only buffers immediately`() {
        val target = ByteBuffer.allocate(8).asReadOnlyBuffer()

        assertThrows(ReadOnlyBufferException::class.java) {
            ByteBufferOutputStream.fixed(target)
        }
    }

    @Test
    fun `fixed Java entry point rejects null before factory logic`() {
        val method = ByteBufferOutputStream::class.java.getMethod("fixed", ByteBuffer::class.java)

        val invocation = assertThrows(InvocationTargetException::class.java) {
            method.invoke(null, null)
        }

        assertSame(NullPointerException::class.java, invocation.cause?.javaClass)
    }

    private fun ByteBuffer.bytesBetween(start: Int, end: Int): ByteArray =
        duplicate()
            .position(start)
            .limit(end)
            .slice()
            .let { view -> ByteArray(view.remaining()).also(view::get) }
}
