package io.bluetape4k.io

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
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

                target.position() shouldBeEqualTo start + payload.size
                target.capacity() shouldBeEqualTo originalCapacity
                target.limit() shouldBeEqualTo originalLimit
                target.order() shouldBeEqualTo originalOrder
                target.bytesBetween(start, start + payload.size) shouldContentEqual payload

                val positionBeforeCopy = target.position()
                target.reset()
                target.position() shouldBeEqualTo start
                target.position(positionBeforeCopy)
                stream.toByteArray() shouldContentEqual payload
                target.position() shouldBeEqualTo positionBeforeCopy

                stream.write(50)
                target.position() shouldBeEqualTo positionBeforeCopy + 1
                stream.toByteArray() shouldContentEqual byteArrayOf(10, 20, 30, 40, 50)
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

        target.position() shouldBeEqualTo 6
        stream.toByteArray() shouldContentEqual byteArrayOf(1, 2, 3, 4)
        target.get(0) shouldBeEqualTo 99
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

        val failure = assertFailsWith<BufferOverflowException> {
            stream.write(byteArrayOf(1, 2, 3, 4))
        }

        failure.javaClass shouldBeEqualTo BufferOverflowException::class.java
        target.position() shouldBeEqualTo start
        target.get(0) shouldBeEqualTo 91
        target.duplicate().limit(target.capacity()).get(6) shouldBeEqualTo 92
        stream.toByteArray() shouldContentEqual byteArrayOf()
    }

    @Test
    fun `fixed factory rejects read-only buffers immediately`() {
        val target = ByteBuffer.allocate(8).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> {
            ByteBufferOutputStream.fixed(target)
        }
    }

    @Test
    fun `fixed Java entry point rejects null before factory logic`() {
        val method = ByteBufferOutputStream::class.java.getMethod("fixed", ByteBuffer::class.java)

        val invocation = assertFailsWith<InvocationTargetException> {
            method.invoke(null, null)
        }

        invocation.cause?.javaClass shouldBeSameInstanceAs NullPointerException::class.java
    }

    private fun ByteBuffer.bytesBetween(start: Int, end: Int): ByteArray =
        duplicate()
            .position(start)
            .limit(end)
            .slice()
            .let { view -> ByteArray(view.remaining()).also(view::get) }
}
