package io.bluetape4k.io.compressor

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdException
import io.bluetape4k.assertions.assertFails
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

class ZstdCompressorByteBufferTest {
    private val compressor = ZstdCompressor()

    @Test
    fun `matched heap and direct storage use native paths and preserve wire compatibility`() {
        val payload = CompressorByteBufferTestSupport.payload
        val expectedWire = compressor.compress(payload)
        val targetCapacity = Int.SIZE_BYTES + Zstd.compressBound(payload.size.toLong()).toInt()

        listOf(false, true).forEach { direct ->
            val source = if (direct) {
                CompressorByteBufferTestSupport.directSlice(payload)
            } else {
                CompressorByteBufferTestSupport.heapSlice(payload)
            }
            val sourceStart = source.position()
            val target = CompressorByteBufferTestSupport.slicedTarget(targetCapacity, direct)
            val targetStart = target.position()

            val written = compressor.compress(source, target)

            written shouldBeEqualTo expectedWire.size
            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart + written
            CompressorByteBufferTestSupport.bytes(target, targetStart, written)
                .shouldContentEqual(expectedWire)

            val wire = if (direct) {
                CompressorByteBufferTestSupport.directSlice(expectedWire)
            } else {
                CompressorByteBufferTestSupport.heapSlice(expectedWire)
            }
            val restored = CompressorByteBufferTestSupport.slicedTarget(payload.size, direct)
            val restoredStart = restored.position()

            compressor.decompress(wire, restored) shouldBeEqualTo payload.size
            CompressorByteBufferTestSupport.bytes(restored, restoredStart, payload.size)
                .shouldContentEqual(payload)
        }
    }

    @Test
    fun `mixed storage and read-only heap source use compatibility fallback`() {
        val payload = CompressorByteBufferTestSupport.payload
        val expectedWire = compressor.compress(payload)
        val operations = RecordingZstdBufferOperations()
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val sources = listOf(
            CompressorByteBufferTestSupport.heap(payload) to true,
            CompressorByteBufferTestSupport.direct(payload) to false,
            CompressorByteBufferTestSupport.heap(payload).asReadOnlyBuffer() to false,
        )

        sources.forEach { (source, directTarget) ->
            val target = CompressorByteBufferTestSupport.writableTarget(expectedWire.size, direct = directTarget)
            val targetStart = target.position()

            testCompressor.compress(source, target) shouldBeEqualTo expectedWire.size
            CompressorByteBufferTestSupport.bytes(target, targetStart, expectedWire.size)
                .shouldContentEqual(expectedWire)
        }

        val compressedSources = listOf(
            CompressorByteBufferTestSupport.heap(expectedWire) to true,
            CompressorByteBufferTestSupport.direct(expectedWire) to false,
            CompressorByteBufferTestSupport.heap(expectedWire).asReadOnlyBuffer() to false,
        )
        compressedSources.forEach { (source, directTarget) ->
            val target = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = directTarget)
            val targetStart = target.position()

            testCompressor.decompress(source, target) shouldBeEqualTo payload.size
            CompressorByteBufferTestSupport.bytes(target, targetStart, payload.size)
                .shouldContentEqual(payload)
        }

        operations.compressInvocations.get() shouldBeEqualTo 0
        operations.decompressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `compression passes exact payload capacity after the four-byte header`() {
        val operations = RecordingZstdBufferOperations(compressResult = 1)
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)

        (0..Int.SIZE_BYTES).forEach { capacity ->
            assertFailsWith<BufferOverflowException> {
                testCompressor.compress(
                    CompressorByteBufferTestSupport.direct(byteArrayOf(1)),
                    CompressorByteBufferTestSupport.writableTarget(capacity, direct = true),
                )
            }
        }

        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
        val payloadCapacity = Zstd.compressBound(source.remaining().toLong()).toInt()
        val target = CompressorByteBufferTestSupport.writableTarget(
            Int.SIZE_BYTES + payloadCapacity,
            direct = true,
        )
        testCompressor.compress(source, target) shouldBeEqualTo Int.SIZE_BYTES + 1

        operations.compressionTargetLength shouldBeEqualTo payloadCapacity
        operations.compressionSourceOffset shouldBeEqualTo source.position()
        operations.compressionTargetOffset shouldBeEqualTo target.position() - 1
    }

    @Test
    fun `native zero compression result is rejected for heap and direct paths`() {
        val payload = byteArrayOf(1, 2, 3)

        listOf(false, true).forEach { direct ->
            val operations = RecordingZstdBufferOperations(compressResult = 0)
            val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
            val source = if (direct) {
                CompressorByteBufferTestSupport.direct(payload)
            } else {
                CompressorByteBufferTestSupport.heap(payload)
            }
            val target = CompressorByteBufferTestSupport.writableTarget(
                Int.SIZE_BYTES + Zstd.compressBound(payload.size.toLong()).toInt(),
                direct = direct,
            )
            val sourcePosition = source.position()
            val targetPosition = target.position()
            val payloadCapacity = target.remaining() - Int.SIZE_BYTES

            assertFailsWith<IllegalStateException> {
                testCompressor.compress(source, target)
            }.message shouldBeEqualTo
                    "Zstd compression returned invalid size=0, payloadCapacity=$payloadCapacity"

            source.position() shouldBeEqualTo sourcePosition
            target.position() shouldBeEqualTo targetPosition
            operations.compressInvocations.get() shouldBeEqualTo 1
        }
    }

    @Test
    fun `compression rejects the exact four-byte header-only target boundary`() {
        val operations = RecordingZstdBufferOperations()
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1))
        val target = CompressorByteBufferTestSupport.writableTarget(Int.SIZE_BYTES, direct = true)
        val sourcePosition = source.position()
        val targetPosition = target.position()

        assertFailsWith<BufferOverflowException> {
            testCompressor.compress(source, target)
        }

        source.position() shouldBeEqualTo sourcePosition
        target.position() shouldBeEqualTo targetPosition
        operations.compressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `direct read-only source uses the optimized native path`() {
        val operations = RecordingZstdBufferOperations(compressResult = 1)
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
            .asReadOnlyBuffer()
        val target = CompressorByteBufferTestSupport.writableTarget(
            Int.SIZE_BYTES + Zstd.compressBound(source.remaining().toLong()).toInt(),
            direct = true,
        )
        val sourcePosition = source.position()
        val targetPosition = target.position()

        testCompressor.compress(source, target) shouldBeEqualTo Int.SIZE_BYTES + 1

        source.position() shouldBeEqualTo sourcePosition
        target.position() shouldBeEqualTo targetPosition + Int.SIZE_BYTES + 1
        operations.compressInvocations.get() shouldBeEqualTo 1
    }

    @Test
    fun `heap compression includes backing-array offsets`() {
        val operations = RecordingZstdBufferOperations(compressResult = 1)
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val source = CompressorByteBufferTestSupport.heapSlice(byteArrayOf(1, 2, 3))
        val payloadCapacity = Zstd.compressBound(source.remaining().toLong()).toInt()
        val target = CompressorByteBufferTestSupport.slicedTarget(
            Int.SIZE_BYTES + payloadCapacity,
            direct = false,
        )
        val expectedSourceOffset = source.arrayOffset() + source.position()
        val expectedTargetOffset = target.arrayOffset() + target.position() + Int.SIZE_BYTES

        testCompressor.compress(source, target) shouldBeEqualTo 5

        operations.compressionSourceOffset shouldBeEqualTo expectedSourceOffset
        operations.compressionTargetOffset shouldBeEqualTo expectedTargetOffset
        operations.compressionTargetLength shouldBeEqualTo payloadCapacity
    }

    @Test
    fun `compression maps only destination-too-small and rejects invalid long counts`() {
        val destinationFailure = ZstdException(Zstd.errDstSizeTooSmall(), "small")
        val otherFailure = ZstdException(Zstd.errCorruptionDetected(), "corrupt")
        val nativeTargetCapacity = Int.SIZE_BYTES + Zstd.compressBound(1).toInt()

        val overflow = RecordingZstdBufferOperations(compressFailure = destinationFailure)
        assertFailsWith<BufferOverflowException> {
            ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, overflow).compress(
                CompressorByteBufferTestSupport.direct(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(nativeTargetCapacity, direct = true),
            )
        }

        val identity = RecordingZstdBufferOperations(compressFailure = otherFailure)
        val thrown = assertFails {
            ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, identity).compress(
                CompressorByteBufferTestSupport.direct(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(nativeTargetCapacity, direct = true),
            )
        }
        thrown shouldBeSameInstanceAs otherFailure

        listOf(-1L, Long.MAX_VALUE, (1L shl 32) + 1L).forEach { invalid ->
            val target = CompressorByteBufferTestSupport.writableTarget(nativeTargetCapacity, direct = true)
            val targetStart = target.position()
            assertFailsWith<IllegalStateException> {
                ZstdCompressor.forTesting(
                    ZstdCompressor.DEFAULT_LEVEL,
                    RecordingZstdBufferOperations(compressResult = invalid),
                ).compress(CompressorByteBufferTestSupport.direct(byteArrayOf(1)), target)
            }
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `decompression validates header and target before native decode`() {
        val operations = RecordingZstdBufferOperations()
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)

        assertFailsWith<IndexOutOfBoundsException> {
            testCompressor.decompress(
                CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3)),
                ByteBuffer.allocateDirect(32),
            )
        }.message shouldBeEqualTo "Zstd header requires 4 bytes"
        assertFailsWith<IllegalArgumentException> {
            testCompressor.decompress(directWire(-1, ByteArray(1)), ByteBuffer.allocateDirect(32))
        }.message shouldBeEqualTo "sourceSize must not be negative: -1"
        assertFailsWith<IllegalArgumentException> {
            testCompressor.decompress(
                directWire(256 * 1024 * 1024 + 1, ByteArray(1)),
                ByteBuffer.allocateDirect(32),
            )
        }.message shouldBeEqualTo "sourceSize exceeds 256 MiB: 268435457"
        assertFailsWith<BufferOverflowException> {
            testCompressor.decompress(validHeaderWire(33), ByteBuffer.allocateDirect(32))
        }

        operations.decompressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `under-declared direct header never expands beyond declared destination`() {
        val operations = RecordingZstdBufferOperations().apply {
            decompressFailure = ZstdException(Zstd.errDstSizeTooSmall(), "Destination too small")
        }
        val compressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val source = directWire(declaredSize = 8, payload = ByteArray(64))
        val target = ByteBuffer.allocateDirect(1024)

        val failure = assertFailsWith<IllegalStateException> {
            compressor.decompress(source, target)
        }

        failure.message shouldBeEqualTo "Zstd decompressed payload exceeds declared size=8"
        failure.cause.shouldBeNull()
        operations.decompressionTargetLength shouldBeEqualTo 8
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `non-destination ZstdException keeps identity`() {
        val expected = ZstdException(Zstd.errCorruptionDetected(), "corrupt")
        val compressor = ZstdCompressor.forTesting(
            ZstdCompressor.DEFAULT_LEVEL,
            RecordingZstdBufferOperations().apply { decompressFailure = expected },
        )

        val thrown = assertFails {
            compressor.decompress(validHeaderWire(32), ByteBuffer.allocateDirect(32))
        }

        thrown shouldBeSameInstanceAs expected
    }

    @Test
    fun `under-declared heap header uses the declared destination bound`() {
        val operations = RecordingZstdBufferOperations().apply {
            decompressFailure = ZstdException(Zstd.errDstSizeTooSmall(), "Destination too small")
        }
        val testCompressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val wire = ByteBuffer.allocate(68).apply {
            putInt(8)
            put(ByteArray(64))
            flip()
        }
        val target = ByteBuffer.allocate(1024)

        val failure = assertFailsWith<IllegalStateException> {
            testCompressor.decompress(wire, target)
        }

        failure.message shouldBeEqualTo "Zstd decompressed payload exceeds declared size=8"
        failure.cause.shouldBeNull()
        operations.decompressionTargetLength shouldBeEqualTo 8
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `over-declared wire is rejected consistently across source and target storage`() {
        val payload = CompressorByteBufferTestSupport.payload
        val declaredSize = payload.size + 3
        val wire = wireWithDeclaredSize(payload, declaredSize)

        CompressorByteBufferTestSupport.sources(wire).forEach { (_, source) ->
            CompressorByteBufferTestSupport.targets(declaredSize).forEach { (_, target) ->
                val sourcePosition = source.position()
                val sourceLimit = source.limit()
                val targetPosition = target.position()
                val targetLimit = target.limit()

                val failure = assertFailsWith<IllegalStateException> {
                    compressor.decompress(source, target)
                }

                failure.message shouldBeEqualTo
                        "Zstd decompressed size mismatch: expected=$declaredSize, actual=${payload.size}"
                source.position() shouldBeEqualTo sourcePosition
                source.limit() shouldBeEqualTo sourceLimit
                target.position() shouldBeEqualTo targetPosition
                target.limit() shouldBeEqualTo targetLimit
                CompressorByteBufferTestSupport.assertMark(source, sourcePosition)
                CompressorByteBufferTestSupport.assertMark(target, targetPosition)
            }
        }
    }

    @Test
    fun `under-declared wire is rejected consistently across source and target storage`() {
        val payload = CompressorByteBufferTestSupport.payload
        val declaredSize = payload.size - 1
        val wire = wireWithDeclaredSize(payload, declaredSize)

        CompressorByteBufferTestSupport.sources(wire).forEach { (_, source) ->
            CompressorByteBufferTestSupport.targets(payload.size).forEach { (_, target) ->
                val sourcePosition = source.position()
                val sourceLimit = source.limit()
                val targetPosition = target.position()
                val targetLimit = target.limit()

                val failure = assertFailsWith<IllegalStateException> {
                    compressor.decompress(source, target)
                }

                failure.message shouldBeEqualTo
                        "Zstd decompressed payload exceeds declared size=$declaredSize"
                source.position() shouldBeEqualTo sourcePosition
                source.limit() shouldBeEqualTo sourceLimit
                target.position() shouldBeEqualTo targetPosition
                target.limit() shouldBeEqualTo targetLimit
                CompressorByteBufferTestSupport.assertMark(source, sourcePosition)
                CompressorByteBufferTestSupport.assertMark(target, targetPosition)
            }
        }
    }

    @Test
    fun `successful decode must exactly match the declared size before narrowing`() {
        listOf(-1L, 7L, 9L, Long.MAX_VALUE, (1L shl 32) + 8L).forEach { actual ->
            val target = ByteBuffer.allocateDirect(32)
            val failure = assertFailsWith<IllegalStateException> {
                ZstdCompressor.forTesting(
                    ZstdCompressor.DEFAULT_LEVEL,
                    RecordingZstdBufferOperations(decompressResult = actual),
                ).decompress(validHeaderWire(8), target)
            }

            failure.message shouldBeEqualTo
                    "Zstd decompressed size mismatch: expected=8, actual=$actual"
            failure.cause.shouldBeNull()
            target.position() shouldBeEqualTo 0
        }
    }

    @Test
    fun `target byte order does not change the wire header`() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val target = CompressorByteBufferTestSupport.writableTarget(
            Int.SIZE_BYTES + Zstd.compressBound(payload.size.toLong()).toInt(),
            direct = true,
        ).order(ByteOrder.LITTLE_ENDIAN)
        val start = target.position()

        compressor.compress(CompressorByteBufferTestSupport.direct(payload), target)

        getIntBigEndian(target, start) shouldBeEqualTo payload.size
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
    }

    @Test
    fun `truncated and corrupt native payloads fail without committing target position`() {
        val payload = CompressorByteBufferTestSupport.payload
        val validWire = compressor.compress(payload)
        val invalidWires = listOf(
            validWire.copyOf(validWire.size - 1),
            byteArrayOf(0, 0, 0, 32, 1, 2, 3, 4),
        )

        invalidWires.forEach { wire ->
            val target = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
            val targetStart = target.position()

            assertFails {
                compressor.decompress(CompressorByteBufferTestSupport.direct(wire), target)
            }
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `overflow keeps caller position and allows retry with a valid wire`() {
        val payload = CompressorByteBufferTestSupport.payload
        val wire = compressor.compress(payload)
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size - 1, direct = true)
        val targetStart = target.position()

        assertFailsWith<BufferOverflowException> {
            compressor.decompress(CompressorByteBufferTestSupport.direct(wire), target)
        }
        target.position() shouldBeEqualTo targetStart

        target.limit(target.capacity())
        compressor.decompress(CompressorByteBufferTestSupport.direct(wire), target)
            .shouldBeEqualTo(payload.size)
        CompressorByteBufferTestSupport.bytes(target, targetStart, payload.size)
            .shouldContentEqual(payload)
    }

    @Test
    fun `singleton matched-storage operations are safe under concurrency`() {
        val payload = CompressorByteBufferTestSupport.payload
        val wireCapacity = Int.SIZE_BYTES + Zstd.compressBound(payload.size.toLong()).toInt()

        MultithreadingTester()
            .workers(8)
            .rounds(2)
            .add {
                val wire = CompressorByteBufferTestSupport.writableTarget(wireCapacity, direct = true)
                val wireStart = wire.position()
                val written = Compressors.Zstd.compress(
                    CompressorByteBufferTestSupport.direct(payload),
                    wire,
                )
                val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
                val restoredStart = restored.position()
                Compressors.Zstd.decompress(
                    wire.duplicate().position(wireStart).limit(wireStart + written),
                    restored,
                )
                CompressorByteBufferTestSupport.bytes(restored, restoredStart, payload.size)
                    .shouldContentEqual(payload)
            }
            .run()
    }

    private fun directWire(declaredSize: Int, payload: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(Int.SIZE_BYTES + payload.size).apply {
            putInt(declaredSize)
            put(payload)
            flip()
        }

    private fun validHeaderWire(declaredSize: Int): ByteBuffer =
        directWire(
            declaredSize,
            byteArrayOf(0x28.toByte(), 0xB5.toByte(), 0x2F.toByte(), 0xFD.toByte()),
        )

    private fun wireWithDeclaredSize(payload: ByteArray, declaredSize: Int): ByteArray =
        compressor.compress(payload).also { wire ->
            ByteBuffer.wrap(wire).putInt(declaredSize)
        }

    private class RecordingZstdBufferOperations(
        private val compressResult: Long = 1,
        private val decompressResult: Long? = null,
        private val compressFailure: ZstdException? = null,
    ): ZstdBufferOperations {
        var decompressFailure: ZstdException? = null
        var decompressionTargetLength: Int? = null
        var compressionTargetLength: Int? = null
        var compressionSourceOffset: Int? = null
        var compressionTargetOffset: Int? = null
        val compressInvocations = AtomicInteger()
        val decompressInvocations = AtomicInteger()

        override fun compressDirect(
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
            source: ByteBuffer,
            sourceOffset: Int,
            sourceLength: Int,
            level: Int,
        ): Long {
            compressInvocations.incrementAndGet()
            compressionTargetLength = targetLength
            compressionSourceOffset = sourceOffset
            compressionTargetOffset = targetOffset
            compressFailure?.let { throw it }
            if (compressResult > 0 && compressResult <= targetLength) {
                target.put(targetOffset, 0x2A)
            }
            return compressResult
        }

        override fun decompressDirect(
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
            source: ByteBuffer,
            sourceOffset: Int,
            sourceLength: Int,
        ): Long {
            decompressInvocations.incrementAndGet()
            decompressionTargetLength = targetLength
            decompressFailure?.let { throw it }
            return decompressResult ?: targetLength.toLong()
        }

        override fun compressHeap(
            target: ByteArray,
            targetOffset: Int,
            targetLength: Int,
            source: ByteArray,
            sourceOffset: Int,
            sourceLength: Int,
            level: Int,
        ): Long {
            compressInvocations.incrementAndGet()
            compressionTargetLength = targetLength
            compressionSourceOffset = sourceOffset
            compressionTargetOffset = targetOffset
            compressFailure?.let { throw it }
            if (compressResult > 0 && compressResult <= targetLength) {
                target[targetOffset] = 0x2A
            }
            return compressResult
        }

        override fun decompressHeap(
            target: ByteArray,
            targetOffset: Int,
            targetLength: Int,
            source: ByteArray,
            sourceOffset: Int,
            sourceLength: Int,
        ): Long {
            decompressInvocations.incrementAndGet()
            decompressionTargetLength = targetLength
            decompressFailure?.let { throw it }
            return decompressResult ?: targetLength.toLong()
        }
    }
}
