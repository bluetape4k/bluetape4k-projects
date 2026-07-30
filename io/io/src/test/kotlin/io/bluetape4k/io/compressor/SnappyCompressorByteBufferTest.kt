package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import org.xerial.snappy.Snappy
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

class SnappyCompressorByteBufferTest {
    private val compressor = SnappyCompressor()

    @Test
    fun `safe direct storage pair dispatches to the Snappy buffer backend`() {
        val operations = RecordingSnappyBufferOperations()
        val compressor = SnappyCompressor.forTesting(operations)
        val payload = byteArrayOf(1, 2, 3)

        listOf(false, true).forEach { direct ->
            val target = CompressorByteBufferTestSupport.writableTarget(16, direct)
            val targetStart = target.position()

            val written = compressor.compress(
                if (direct) {
                    CompressorByteBufferTestSupport.direct(payload)
                } else {
                    CompressorByteBufferTestSupport.heap(payload)
                },
                target,
            )

            written shouldBeEqualTo if (direct) 1 else compressor.compress(payload).size
            target.position() shouldBeEqualTo targetStart + written
        }

        operations.compressInvocations.get() shouldBeEqualTo 1
    }

    @Test
    fun `direct caller-owned APIs preserve wire compatibility and caller state`() {
        val payload = CompressorByteBufferTestSupport.payload
        val expectedWire = compressor.compress(payload)
        val source = CompressorByteBufferTestSupport.direct(payload)
        val sourceStart = source.position()
        val sourceLimit = source.limit()
        val compressed = CompressorByteBufferTestSupport.writableTarget(
            Snappy.maxCompressedLength(payload.size),
            direct = true,
        )
        val compressedStart = compressed.position()

        val written = compressor.compress(source, compressed)

        written shouldBeEqualTo expectedWire.size
        source.position() shouldBeEqualTo sourceStart
        source.limit() shouldBeEqualTo sourceLimit
        CompressorByteBufferTestSupport.assertMark(source, sourceStart)
        compressed.position() shouldBeEqualTo compressedStart + written
        CompressorByteBufferTestSupport.bytes(compressed, compressedStart, written)
            .shouldContentEqual(expectedWire)

        val restoredSource = CompressorByteBufferTestSupport.direct(expectedWire)
        val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        val restoredStart = restored.position()

        compressor.decompress(restoredSource, restored) shouldBeEqualTo payload.size
        restored.position() shouldBeEqualTo restoredStart + payload.size
        CompressorByteBufferTestSupport.bytes(restored, restoredStart, payload.size)
            .shouldContentEqual(payload)
    }

    @Test
    fun `direct compression below the native safety bound keeps compatibility behavior`() {
        val operations = RecordingSnappyBufferOperations(maxCompressedLength = 64)
        val compressor = SnappyCompressor.forTesting(operations)
        val payload = byteArrayOf(1, 2, 3)
        val expectedWire = compressor.compress(payload)
        val target = CompressorByteBufferTestSupport.writableTarget(expectedWire.size, direct = true)
        val targetStart = target.position()

        compressor.compress(CompressorByteBufferTestSupport.direct(payload), target)
            .shouldBeEqualTo(expectedWire.size)

        operations.compressInvocations.get() shouldBeEqualTo 0
        CompressorByteBufferTestSupport.bytes(target, targetStart, expectedWire.size)
            .shouldContentEqual(expectedWire)
    }

    @Test
    fun `common preflight rejects read-only and empty calls before backend bounds`() {
        val operations = RecordingSnappyBufferOperations()
        val compressor = SnappyCompressor.forTesting(operations)

        compressor.compress(
            ByteBuffer.allocateDirect(0),
            ByteBuffer.allocateDirect(1),
        ) shouldBeEqualTo 0
        assertFailsWith<ReadOnlyBufferException> {
            compressor.compress(
                CompressorByteBufferTestSupport.direct(byteArrayOf(1)),
                ByteBuffer.allocateDirect(8).asReadOnlyBuffer(),
            )
        }

        operations.maxCompressedLengthInvocations.get() shouldBeEqualTo 0
        operations.compressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `invalid direct payload is rejected before native decode`() {
        val operations = RecordingSnappyBufferOperations(valid = false)
        val compressor = SnappyCompressor.forTesting(operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(32, direct = true)
        val sourceStart = source.position()
        val targetStart = target.position()

        val failure = assertFailsWith<IllegalArgumentException> {
            compressor.decompress(source, target)
        }

        failure.message shouldBeEqualTo "유효하지 않은 Snappy payload입니다."
        operations.decompressInvocations.get() shouldBeEqualTo 0
        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
    }

    @Test
    fun `real invalid direct payload wins over an empty target`() {
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
        val target = ByteBuffer.allocateDirect(0)

        assertFailsWith<IllegalArgumentException> {
            compressor.decompress(source, target)
        }
    }

    @Test
    fun `declared direct payload size is bounded before native decode`() {
        val operations = RecordingSnappyBufferOperations(uncompressedLength = 32)
        val compressor = SnappyCompressor.forTesting(operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1))
        val target = CompressorByteBufferTestSupport.writableTarget(31, direct = true)
        val targetStart = target.position()

        assertFailsWith<BufferOverflowException> {
            compressor.decompress(source, target)
        }

        operations.decompressInvocations.get() shouldBeEqualTo 0
        target.position() shouldBeEqualTo targetStart
    }

    @Test
    fun `direct decompression isolates inspection views and bounds native output exactly`() {
        val operations = RecordingSnappyBufferOperations(
            uncompressedLength = 2,
            consumeInspectionViews = true,
        )
        val compressor = SnappyCompressor.forTesting(operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(16, direct = true)

        compressor.decompress(source, target) shouldBeEqualTo 2

        operations.inspectedSourceRemaining shouldBeEqualTo listOf(3, 3, 3)
        operations.decompressionTargetRemaining.get() shouldBeEqualTo 2
    }

    @Test
    fun `backend failures preserve throwable identity and caller positions`() {
        val expected = CancellationException("cancelled")
        val operations = RecordingSnappyBufferOperations(compressFailure = expected)
        val compressor = SnappyCompressor.forTesting(operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(16, direct = true)
        val sourceStart = source.position()
        val targetStart = target.position()

        val actual = assertFailsWith<Throwable> {
            compressor.compress(source, target)
        }

        actual shouldBeSameInstanceAs expected
        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
    }

    @Test
    fun `singleton native direct operations are safe under concurrency`() {
        val payload = CompressorByteBufferTestSupport.payload

        MultithreadingTester()
            .workers(8)
            .rounds(2)
            .add {
                val wire = CompressorByteBufferTestSupport.writableTarget(
                    Snappy.maxCompressedLength(payload.size),
                    direct = true,
                )
                val wireStart = wire.position()
                val written = Compressors.Snappy.compress(
                    CompressorByteBufferTestSupport.direct(payload),
                    wire,
                )
                val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
                Compressors.Snappy.decompress(
                    wire.duplicate().position(wireStart).limit(wireStart + written),
                    restored,
                )
                CompressorByteBufferTestSupport.bytes(restored, 5, payload.size)
                    .shouldContentEqual(payload)
            }
            .run()
    }

    private class RecordingSnappyBufferOperations(
        private val maxCompressedLength: Int = 4,
        private val valid: Boolean = true,
        private val uncompressedLength: Int = 1,
        private val compressFailure: Throwable? = null,
        private val consumeInspectionViews: Boolean = false,
    ): SnappyBufferOperations {
        val maxCompressedLengthInvocations = AtomicInteger()
        val compressInvocations = AtomicInteger()
        val decompressInvocations = AtomicInteger()
        val decompressionTargetRemaining = AtomicInteger()
        val inspectedSourceRemaining = mutableListOf<Int>()

        override fun maxCompressedLength(sourceLength: Int): Int {
            maxCompressedLengthInvocations.incrementAndGet()
            return maxCompressedLength
        }

        override fun compress(source: ByteBuffer, target: ByteBuffer): Int {
            compressInvocations.incrementAndGet()
            compressFailure?.let { throw it }
            target.put(0x2A)
            return 1
        }

        override fun isValidCompressedBuffer(source: ByteBuffer): Boolean {
            inspectedSourceRemaining += source.remaining()
            if (consumeInspectionViews) source.position(source.limit())
            return valid
        }

        override fun uncompressedLength(source: ByteBuffer): Int {
            inspectedSourceRemaining += source.remaining()
            if (consumeInspectionViews) source.position(source.limit())
            return uncompressedLength
        }

        override fun decompress(source: ByteBuffer, target: ByteBuffer): Int {
            decompressInvocations.incrementAndGet()
            inspectedSourceRemaining += source.remaining()
            decompressionTargetRemaining.set(target.remaining())
            repeat(uncompressedLength) { target.put(0x2A) }
            return uncompressedLength
        }
    }
}
