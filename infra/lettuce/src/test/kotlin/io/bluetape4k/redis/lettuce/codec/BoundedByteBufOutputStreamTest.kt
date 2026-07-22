package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer

class BoundedByteBufOutputStreamTest {

    @Test
    fun `writes from the construction writer index across supported target kinds`() {
        targetFactories().forEach { (name, factory) ->
            val target = factory()
            try {
                target.setByte(0, 0x5A)
                target.writerIndex(1)
                val output = BoundedByteBufOutputStream(target)

                output.write(0x11)
                output.write(byteArrayOf(0x22, 0x33))
                output.write(byteArrayOf(0x44, 0x55, 0x66), 1, 2)

                output.startIndex() shouldBeEqualTo 1
                output.writtenBytes() shouldBeEqualTo 5
                target.writerIndex() shouldBeEqualTo 1
                target.readerIndex() shouldBeEqualTo 0
                target.refCnt() shouldBeEqualTo 1
                target.bytes(0, 6).contentEquals(
                    byteArrayOf(0x5A, 0x11, 0x22, 0x33, 0x55, 0x66),
                ).shouldBeTrue()
            } catch (failure: Throwable) {
                throw AssertionError("target fixture failed: $name", failure)
            } finally {
                target.release()
            }
        }
    }

    @Test
    fun `writes at the exact fixed capacity boundary`() {
        val target = Unpooled.buffer(4, 4)
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            output.write(byteArrayOf(1, 2, 3))

            output.writtenBytes() shouldBeEqualTo 3
            target.capacity() shouldBeEqualTo 4
            target.writerIndex() shouldBeEqualTo 1
            target.bytes(0, 4).contentEquals(byteArrayOf(0x5A, 1, 2, 3)).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `grows capacity cumulatively across multiple chunks`() {
        val target = Unpooled.buffer(2, 32)
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            output.write(byteArrayOf(1, 2, 3))
            val firstCapacity = target.capacity()
            output.write(byteArrayOf(4, 5, 6, 7))

            (firstCapacity >= 4).shouldBeTrue()
            (target.capacity() >= 8).shouldBeTrue()
            output.writtenBytes() shouldBeEqualTo 7
            target.writerIndex() shouldBeEqualTo 1
            target.bytes(1, 7).contentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7)).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `rejects max capacity exhaustion before target mutation`() {
        val target = Unpooled.buffer(4, 6)
        try {
            target.setBytes(0, byteArrayOf(9, 8, 7, 6))
            target.writerIndex(2)
            val before = target.bytes(0, target.capacity())
            val output = BoundedByteBufOutputStream(target)

            val failure = assertFailsWith<IllegalStateException> {
                output.write(byteArrayOf(1, 2, 3, 4, 5))
            }

            failure.message shouldBeEqualTo "Serialized output exceeds target maxCapacity."
            output.writtenBytes() shouldBeEqualTo 0
            target.capacity() shouldBeEqualTo 4
            target.writerIndex() shouldBeEqualTo 2
            target.bytes(0, target.capacity()).contentEquals(before).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `rejects arithmetic overflow before target mutation`() {
        val target = OverflowSnapshotByteBuf(Unpooled.buffer(1, 1))
        try {
            val before = target.bytes(0, target.capacity())
            val output = BoundedByteBufOutputStream(target)

            assertFailsWith<ArithmeticException> {
                output.write(1)
            }

            output.writtenBytes() shouldBeEqualTo 0
            target.bytes(0, target.capacity()).contentEquals(before).shouldBeTrue()
        } finally {
            target.releaseDelegate()
        }
    }

    @Test
    fun `rejects invalid array ranges before target mutation`() {
        val target = Unpooled.buffer(8, 8)
        try {
            target.setBytes(0, byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2))
            target.writerIndex(2)
            val before = target.bytes(0, target.capacity())
            val output = BoundedByteBufOutputStream(target)

            listOf<() -> Unit>(
                { output.write(byteArrayOf(1, 2, 3), -1, 1) },
                { output.write(byteArrayOf(1, 2, 3), 0, -1) },
                { output.write(byteArrayOf(1, 2, 3), 2, 2) },
                { output.write(byteArrayOf(1, 2, 3), Int.MAX_VALUE, 1) },
            ).forEach { invalidWrite ->
                assertFailsWith<IndexOutOfBoundsException> { invalidWrite() }
            }

            output.writtenBytes() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo 2
            target.bytes(0, target.capacity()).contentEquals(before).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `keeps committed count unchanged after a partial bulk target failure`() {
        val failure = IOException("partial bulk failure")
        val target = PartialFailingBulkByteBuf(Unpooled.buffer(2, 16), failure)
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            val actual = assertFailsWith<IOException> {
                output.write(byteArrayOf(1, 2, 3, 4))
            }

            actual shouldBeSameInstanceAs failure
            output.writtenBytes() shouldBeEqualTo 0
            output.highWaterBytes() shouldBeEqualTo 4
            target.writerIndex() shouldBeEqualTo 1
            (target.capacity() >= 5).shouldBeTrue()
            target.getUnsignedByte(0) shouldBeEqualTo 0x5A
            target.getUnsignedByte(1) shouldBeEqualTo 1
            target.getUnsignedByte(2) shouldBeEqualTo 2
        } finally {
            target.release()
        }
    }

    @Test
    fun `preserves prefix and suffix outside the attempted high water on bulk failure`() {
        val delegate = Unpooled.buffer(8, 8)
        delegate.setBytes(0, ByteArray(8) { 0x7F })
        delegate.writerIndex(2)
        val target = PartialFailingBulkByteBuf(delegate, IOException("partial bulk failure"))
        try {
            val output = BoundedByteBufOutputStream(target)

            assertFailsWith<IOException> {
                output.write(byteArrayOf(1, 2, 3))
            }

            output.writtenBytes() shouldBeEqualTo 0
            output.highWaterBytes() shouldBeEqualTo 3
            target.bytes(0, 2).contentEquals(byteArrayOf(0x7F, 0x7F)).shouldBeTrue()
            target.bytes(5, 3).contentEquals(byteArrayOf(0x7F, 0x7F, 0x7F)).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `a shorter retry never commits dirty suffix from a failed write`() {
        val target = PartialFailingBulkByteBuf(Unpooled.buffer(2, 16), IOException("partial bulk failure"))
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            assertFailsWith<IOException> {
                output.write(byteArrayOf(1, 2, 3, 4))
            }
            target.failBulkWrites = false
            output.write(byteArrayOf(9))
            target.writerIndex(Math.addExact(output.startIndex(), output.writtenBytes()))

            target.readableBytes() shouldBeEqualTo 2
            output.highWaterBytes() shouldBeEqualTo 4
            target.bytes(0, target.writerIndex()).contentEquals(byteArrayOf(0x5A, 9)).shouldBeTrue()
            target.getUnsignedByte(2) shouldBeEqualTo 2
        } finally {
            target.release()
        }
    }

    @Test
    fun `detects writer index drift before writing`() {
        val target = Unpooled.buffer(8, 8)
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)
            target.writerIndex(2)

            val failure = assertFailsWith<IllegalStateException> {
                output.write(1)
            }

            failure.message shouldBeEqualTo "Target ByteBuf state changed during serialization."
            output.writtenBytes() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo 2
        } finally {
            target.release()
        }
    }

    @Test
    fun `detects reader index drift before writing`() {
        val target = Unpooled.buffer(8, 8)
        try {
            target.writeBytes(byteArrayOf(9, 8))
            val output = BoundedByteBufOutputStream(target)
            target.readerIndex(1)

            assertFailsWith<IllegalStateException> {
                output.write(1)
            }

            output.writtenBytes() shouldBeEqualTo 0
            target.readerIndex() shouldBeEqualTo 1
        } finally {
            target.release()
        }
    }

    @Test
    fun `detects reference count drift without compensating lifecycle calls`() {
        val target = LifecycleTrackingByteBuf(Unpooled.buffer(8, 8))
        try {
            target.retain()
            target.resetCounts()
            val output = BoundedByteBufOutputStream(target)
            target.retain()
            target.resetCounts()

            assertFailsWith<IllegalStateException> {
                output.write(1)
            }

            target.retainCalls shouldBeEqualTo 0
            target.releaseCalls shouldBeEqualTo 0
            target.refCnt() shouldBeEqualTo 3
        } finally {
            target.releaseDelegate(3)
        }
    }

    @Test
    fun `detects state drift caused while ensuring writable capacity`() {
        val target = EnsureWritableDriftByteBuf(Unpooled.buffer(1, 8))
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            assertFailsWith<IllegalStateException> {
                output.write(byteArrayOf(1, 2))
            }

            output.writtenBytes() shouldBeEqualTo 0
            target.readerIndex() shouldBeEqualTo 1
            target.getUnsignedByte(0) shouldBeEqualTo 0x5A
        } finally {
            target.release()
        }
    }

    @Test
    fun `verify snapshot fails closed after external drift`() {
        val target = Unpooled.buffer(8, 8)
        try {
            val output = BoundedByteBufOutputStream(target)
            output.verifySnapshot()
            target.writerIndex(1)

            assertFailsWith<IllegalStateException> {
                output.verifySnapshot()
            }

            target.writerIndex() shouldBeEqualTo 1
        } finally {
            target.release()
        }
    }

    @Test
    fun `preserves reader and writer marks`() {
        val target = Unpooled.buffer(16, 16)
        try {
            target.writeBytes(byteArrayOf(9, 8, 7, 6))
            target.readerIndex(1)
            target.markReaderIndex()
            target.markWriterIndex()
            val output = BoundedByteBufOutputStream(target)

            output.write(byteArrayOf(1, 2, 3))
            target.readerIndex(2)
            target.resetReaderIndex()
            target.resetWriterIndex()

            target.readerIndex() shouldBeEqualTo 1
            target.writerIndex() shouldBeEqualTo 4
            output.writtenBytes() shouldBeEqualTo 3
        } finally {
            target.release()
        }
    }

    @Test
    fun `flush and close are caller lifecycle no-ops`() {
        val target = LifecycleTrackingByteBuf(Unpooled.buffer(8, 8))
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            output.flush()
            output.close()

            target.retainCalls shouldBeEqualTo 0
            target.releaseCalls shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo 1
            target.readerIndex() shouldBeEqualTo 0
            target.refCnt() shouldBeEqualTo 1
        } finally {
            target.releaseDelegate()
        }
    }

    @Test
    fun `seal rejects all later writes but keeps flush and close as no-ops`() {
        val target = Unpooled.buffer(8, 8)
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)
            output.seal()

            val singleFailure = assertFailsWith<IOException> { output.write(1) }
            val bulkFailure = assertFailsWith<IOException> { output.write(byteArrayOf(1, 2, 3)) }
            output.flush()
            output.close()

            singleFailure.message shouldBeEqualTo "Bounded ByteBuf output stream is sealed."
            bulkFailure.message shouldBeEqualTo "Bounded ByteBuf output stream is sealed."
            output.writtenBytes() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo 1
            target.getUnsignedByte(0) shouldBeEqualTo 0x5A
        } finally {
            target.release()
        }
    }

    @Test
    fun `never requests a mutable NIO view`() {
        val target = RejectingNioByteBuf(Unpooled.buffer(2, 16))
        try {
            target.writeByte(0x5A)
            val output = BoundedByteBufOutputStream(target)

            output.write(byteArrayOf(1, 2, 3, 4))

            target.nioCalls shouldBeEqualTo 0
            output.writtenBytes() shouldBeEqualTo 4
            target.writerIndex() shouldBeEqualTo 1
            target.bytes(1, 4).contentEquals(byteArrayOf(1, 2, 3, 4)).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    private fun targetFactories(): List<Pair<String, () -> ByteBuf>> =
        listOf(
            "unpooled heap" to { Unpooled.buffer(4, 16) },
            "unpooled direct" to { Unpooled.directBuffer(4, 16) },
            "pooled heap" to { PooledByteBufAllocator.DEFAULT.heapBuffer(4, 16) },
            "pooled direct" to { PooledByteBufAllocator.DEFAULT.directBuffer(4, 16) },
            "slice" to { Unpooled.buffer(16, 16).slice(0, 16) },
            "composite" to {
                Unpooled.compositeBuffer().addComponents(
                    true,
                    Unpooled.buffer(4, 4).writeZero(4),
                    Unpooled.buffer(4, 4).writeZero(4),
                )
            },
        )

    private fun ByteBuf.bytes(index: Int, length: Int): ByteArray =
        ByteArray(length).also { bytes -> getBytes(index, bytes) }

    @Suppress("DEPRECATION")
    private class PartialFailingBulkByteBuf(
        delegate: ByteBuf,
        private val failure: IOException,
    ): io.netty.buffer.DuplicatedByteBuf(delegate) {
        var failBulkWrites: Boolean = true

        override fun setBytes(index: Int, source: ByteArray, sourceIndex: Int, length: Int): ByteBuf {
            if (!failBulkWrites) return super.setBytes(index, source, sourceIndex, length)
            super.setBytes(index, source, sourceIndex, minOf(2, length))
            throw failure
        }
    }

    @Suppress("DEPRECATION")
    private class EnsureWritableDriftByteBuf(
        delegate: ByteBuf,
    ): io.netty.buffer.DuplicatedByteBuf(delegate) {
        override fun ensureWritable(minWritableBytes: Int): ByteBuf {
            super.ensureWritable(minWritableBytes)
            readerIndex(readerIndex() + 1)
            return this
        }
    }

    @Suppress("DEPRECATION")
    private class LifecycleTrackingByteBuf(
        private val delegate: ByteBuf,
    ): io.netty.buffer.SwappedByteBuf(delegate) {
        var retainCalls: Int = 0
            private set
        var releaseCalls: Int = 0
            private set

        override fun retain(): ByteBuf {
            retainCalls++
            return super.retain()
        }

        override fun retain(increment: Int): ByteBuf {
            retainCalls++
            return super.retain(increment)
        }

        override fun release(): Boolean {
            releaseCalls++
            return super.release()
        }

        override fun release(decrement: Int): Boolean {
            releaseCalls++
            return super.release(decrement)
        }

        fun resetCounts() {
            retainCalls = 0
            releaseCalls = 0
        }

        fun releaseDelegate(decrement: Int = 1) {
            delegate.release(decrement)
        }
    }

    @Suppress("DEPRECATION")
    private class RejectingNioByteBuf(delegate: ByteBuf): io.netty.buffer.DuplicatedByteBuf(delegate) {
        var nioCalls: Int = 0
            private set

        override fun nioBuffer(): ByteBuffer {
            nioCalls++
            throw AssertionError("bounded writer must not request a NIO view")
        }

        override fun nioBuffer(index: Int, length: Int): ByteBuffer {
            nioCalls++
            throw AssertionError("bounded writer must not request a NIO view")
        }

        override fun internalNioBuffer(index: Int, length: Int): ByteBuffer {
            nioCalls++
            throw AssertionError("bounded writer must not request a NIO view")
        }
    }

    @Suppress("DEPRECATION")
    private class OverflowSnapshotByteBuf(
        private val delegate: ByteBuf,
    ): io.netty.buffer.DuplicatedByteBuf(delegate) {
        override fun writerIndex(): Int = Int.MAX_VALUE

        override fun maxCapacity(): Int = Int.MAX_VALUE

        fun releaseDelegate() {
            delegate.release()
        }
    }
}
