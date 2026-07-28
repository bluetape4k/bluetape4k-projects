package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import net.jpountz.lz4.LZ4Exception
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

class LZ4CompressorByteBufferTest {
    private val compressor = LZ4Compressor()

    @Test
    fun `caller-owned compression and decompression support all storage combinations`() {
        val payload = CompressorByteBufferTestSupport.payload
        val wire = compressor.compress(payload)

        verifyStorageMatrix(payload, wire) { source, target ->
            compressor.compress(source, target)
        }
        verifyStorageMatrix(wire, payload) { source, target ->
            compressor.decompress(source, target)
        }
    }

    @Test
    fun `caller-owned and allocating APIs preserve the same wire format`() {
        val payload = CompressorByteBufferTestSupport.payload
        val legacyWire = compressor.compress(payload)

        val decompressed = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        val decompressedStart = decompressed.position()
        compressor.decompress(CompressorByteBufferTestSupport.direct(legacyWire), decompressed)
            .shouldBeEqualTo(payload.size)
        CompressorByteBufferTestSupport.bytes(decompressed, decompressedStart, payload.size)
            .shouldContentEqual(payload)

        val wireTarget = CompressorByteBufferTestSupport.writableTarget(legacyWire.size, direct = true)
        val wireStart = wireTarget.position()
        val written = compressor.compress(CompressorByteBufferTestSupport.heap(payload), wireTarget)
        val callerOwnedWire = CompressorByteBufferTestSupport.bytes(wireTarget, wireStart, written)

        callerOwnedWire.shouldContentEqual(legacyWire)
        compressor.decompress(callerOwnedWire).shouldContentEqual(payload)
    }

    @Test
    fun `caller-owned compression writes big-endian header regardless of target order`() {
        val payload = CompressorByteBufferTestSupport.payload
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, direct = true)
        val targetStart = target.position()

        compressor.compress(CompressorByteBufferTestSupport.heap(payload), target)

        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        getIntBigEndian(target, targetStart) shouldBeEqualTo payload.size
    }

    @Test
    fun `caller-owned compression dispatches direct views with caller offsets`() {
        val payload = CompressorByteBufferTestSupport.payload
        val source = CompressorByteBufferTestSupport.direct(payload)
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, direct = true)
        val targetStart = target.position()
        val operations = object: LZ4BufferOperations {
            override fun compress(
                source: ByteBuffer,
                sourceOffset: Int,
                sourceLength: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int {
                source.isDirect shouldBeEqualTo true
                sourceOffset shouldBeEqualTo 7
                sourceLength shouldBeEqualTo payload.size
                target.isDirect shouldBeEqualTo true
                targetOffset shouldBeEqualTo targetStart + Int.SIZE_BYTES
                targetLength shouldBeEqualTo target.remaining() - Int.SIZE_BYTES
                target.put(targetOffset, 0x01.toByte())
                return 1
            }

            override fun decompress(
                source: ByteBuffer,
                sourceOffset: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int = error("decompression is not expected")
        }

        LZ4Compressor.forTesting(operations).compress(source, target)
            .shouldBeEqualTo(Int.SIZE_BYTES + 1)

        source.position() shouldBeEqualTo 7
        target.position() shouldBeEqualTo targetStart + Int.SIZE_BYTES + 1
        getIntBigEndian(target, targetStart) shouldBeEqualTo payload.size
    }

    @Test
    fun `caller-owned compression rejects header-only and smaller destinations`() {
        (0..Int.SIZE_BYTES).forEach { remaining ->
            val source = CompressorByteBufferTestSupport.heap(CompressorByteBufferTestSupport.payload)
            val target = CompressorByteBufferTestSupport.writableTarget(remaining, direct = remaining % 2 == 0)
            val sourceStart = source.position()
            val targetStart = target.position()

            assertFailsWith<BufferOverflowException> {
                compressor.compress(source, target)
            }

            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `caller-owned compression rejects invalid codec write counts before commit`() {
        listOf(-1, 0, Int.MAX_VALUE).forEach { invalidCount ->
            val compressor = LZ4Compressor.forTesting(returningOperations(compressedSize = invalidCount))
            val source = CompressorByteBufferTestSupport.heap(CompressorByteBufferTestSupport.payload)
            val target = CompressorByteBufferTestSupport.writableTarget(
                CompressorByteBufferTestSupport.payload.size * 2,
                direct = true,
            )
            val sourceStart = source.position()
            val targetStart = target.position()
            val payloadCapacity = target.remaining() - Int.SIZE_BYTES

            val failure = assertFailsWith<IllegalStateException> {
                compressor.compress(source, target)
            }

            failure.message shouldBeEqualTo
                    "LZ4 payload write count out of range: " +
                    "written=$invalidCount, capacity=$payloadCapacity"
            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `caller-owned compression maps only codec destination exhaustion to overflow`() {
        val overflow = LZ4Exception("maxDestLen is too small")
        val corrupt = LZ4Exception("corrupt")
        val source = CompressorByteBufferTestSupport.heap(CompressorByteBufferTestSupport.payload)
        val target = CompressorByteBufferTestSupport.writableTarget(
            CompressorByteBufferTestSupport.payload.size * 2,
            direct = true,
        )

        assertFailsWith<BufferOverflowException> {
            LZ4Compressor.forTesting(throwingOperations(compressFailure = overflow))
                .compress(source, target)
        }
        val actual = assertFailsWith<LZ4Exception> {
            LZ4Compressor.forTesting(throwingOperations(compressFailure = corrupt))
                .compress(source, target)
        }
        actual shouldBeSameInstanceAs corrupt
    }

    @Test
    fun `caller-owned compression target can be reused after overflow`() {
        val largePayload = ByteArray(16 * 1024)
        val largeWire = compressor.compress(largePayload)
        val target = CompressorByteBufferTestSupport.writableTarget(largeWire.size - 1, direct = true)
        val targetStart = target.position()

        assertFailsWith<BufferOverflowException> {
            compressor.compress(CompressorByteBufferTestSupport.direct(largePayload), target)
        }
        target.position() shouldBeEqualTo targetStart

        val retryPayload = ByteArray(512)
        val retryWire = compressor.compress(retryPayload)
        compressor.compress(CompressorByteBufferTestSupport.heap(retryPayload), target)
            .shouldBeEqualTo(retryWire.size)
        CompressorByteBufferTestSupport.bytes(target, targetStart, retryWire.size)
            .shouldContentEqual(retryWire)
    }

    @Test
    fun `caller-owned decompression rejects invalid declared sizes`() {
        listOf(-1, 256 * 1024 * 1024 + 1).forEach { declaredSize ->
            val source = ByteBuffer.wrap(intHeader(declaredSize) + byteArrayOf(0x00))
            val target = CompressorByteBufferTestSupport.writableTarget(32, direct = true)
            val targetStart = target.position()

            assertFailsWith<IllegalArgumentException> {
                compressor.decompress(source, target)
            }

            source.position() shouldBeEqualTo 0
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `caller-owned decompression rejects truncated headers without moving caller state`() {
        (1 until Int.SIZE_BYTES).forEach { sourceSize ->
            val source = CompressorByteBufferTestSupport.direct(ByteArray(sourceSize))
            val target = CompressorByteBufferTestSupport.writableTarget(32, direct = true)
            val sourceStart = source.position()
            val targetStart = target.position()

            assertFailsWith<IndexOutOfBoundsException> {
                compressor.decompress(source, target)
            }

            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `highly compressible wire is rejected before payload-sized decompression`() {
        val payload = ByteArray(16 * 1024)
        val wire = compressor.compress(payload)
        val source = CompressorByteBufferTestSupport.direct(wire)
        val target = CompressorByteBufferTestSupport.writableTarget(1, direct = true)
        val sourceStart = source.position()
        val targetStart = target.position()

        assertFailsWith<BufferOverflowException> {
            compressor.decompress(source, target)
        }

        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
    }

    @Test
    fun `target overflow wins before corrupt payload decode and permits retry`() {
        val decodeInvocations = AtomicInteger()
        val operations = object: LZ4BufferOperations {
            override fun compress(
                source: ByteBuffer,
                sourceOffset: Int,
                sourceLength: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int = error("compression is not expected")

            override fun decompress(
                source: ByteBuffer,
                sourceOffset: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int {
                decodeInvocations.incrementAndGet()
                throw LZ4Exception("corrupt")
            }
        }
        val target = CompressorByteBufferTestSupport.writableTarget(64, direct = true)
        val targetStart = target.position()
        val corrupt = ByteBuffer.wrap(intHeader(target.remaining() + 1) + byteArrayOf(0x00))

        assertFailsWith<BufferOverflowException> {
            LZ4Compressor.forTesting(operations).decompress(corrupt, target)
        }

        decodeInvocations.get() shouldBeEqualTo 0
        target.position() shouldBeEqualTo targetStart

        val retryPayload = "retry after overflow".encodeToByteArray()
        val retryWire = compressor.compress(retryPayload)
        compressor.decompress(ByteBuffer.wrap(retryWire), target).shouldBeEqualTo(retryPayload.size)
        CompressorByteBufferTestSupport.bytes(target, targetStart, retryPayload.size)
            .shouldContentEqual(retryPayload)
    }

    @Test
    fun `decompression never reads valid capacity tail beyond caller limit`() {
        val wire = compressor.compress(CompressorByteBufferTestSupport.payload)
        val source = ByteBuffer.allocateDirect(wire.size).put(wire).flip()
        source.limit(source.limit() - 1)
        val target = ByteBuffer.allocateDirect(CompressorByteBufferTestSupport.payload.size)

        assertFailsWith<LZ4Exception> {
            compressor.decompress(source, target)
        }

        source.position() shouldBeEqualTo 0
        source.limit() shouldBeEqualTo wire.size - 1
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `caller-owned decompression passes a zero-offset capacity-bounded payload view`() {
        val source = ByteBuffer.allocateDirect(24).apply {
            repeat(capacity()) { put(CompressorByteBufferTestSupport.FILL) }
            putInt(4, 1)
            put(8, 0x01.toByte())
            put(9, 0x02.toByte())
            position(4)
            limit(10)
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        }
        val target = CompressorByteBufferTestSupport.writableTarget(8, direct = true)
        val targetStart = target.position()
        val operations = object: LZ4BufferOperations {
            override fun compress(
                source: ByteBuffer,
                sourceOffset: Int,
                sourceLength: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int = error("compression is not expected")

            override fun decompress(
                source: ByteBuffer,
                sourceOffset: Int,
                target: ByteBuffer,
                targetOffset: Int,
                targetLength: Int,
            ): Int {
                source.isDirect shouldBeEqualTo true
                source.position() shouldBeEqualTo 0
                source.limit() shouldBeEqualTo 2
                source.capacity() shouldBeEqualTo 2
                sourceOffset shouldBeEqualTo 0
                target.isDirect shouldBeEqualTo true
                targetOffset shouldBeEqualTo targetStart
                targetLength shouldBeEqualTo 1
                target.put(targetOffset, 0x7F.toByte())
                return source.remaining()
            }
        }

        LZ4Compressor.forTesting(operations).decompress(source, target).shouldBeEqualTo(1)

        source.position() shouldBeEqualTo 4
        source.limit() shouldBeEqualTo 10
        source.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        CompressorByteBufferTestSupport.assertMark(source, 4)
        target.position() shouldBeEqualTo targetStart + 1
    }

    @Test
    fun `caller-owned decompression rejects trailing compressed bytes`() {
        val wire = compressor.compress(CompressorByteBufferTestSupport.payload) + byteArrayOf(0x5A)
        val source = ByteBuffer.wrap(wire)
        val target = ByteBuffer.allocate(CompressorByteBufferTestSupport.payload.size)

        assertFailsWith<LZ4Exception> {
            compressor.decompress(source, target)
        }

        source.position() shouldBeEqualTo 0
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `caller-owned decompression rejects codec consumed-length mismatch`() {
        val source = ByteBuffer.wrap(intHeader(1) + byteArrayOf(0x01, 0x02))
        val target = ByteBuffer.allocate(1)

        val failure = assertFailsWith<LZ4Exception> {
            LZ4Compressor.forTesting(returningOperations(compressedSize = 1, consumedSize = 1))
                .decompress(source, target)
        }

        failure.message shouldBeEqualTo
                "LZ4 compressed payload length mismatch: consumed=1, remaining=2"
        source.position() shouldBeEqualTo 0
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `caller-owned codec failures preserve identity and positions`() {
        val failure = LZ4Exception("pre-created")
        val compressor = LZ4Compressor.forTesting(
            throwingOperations(compressFailure = failure, decompressFailure = failure)
        )
        val plain = CompressorByteBufferTestSupport.heap(CompressorByteBufferTestSupport.payload)
        val compressed = ByteBuffer.wrap(intHeader(1) + byteArrayOf(0x00))
        val target = CompressorByteBufferTestSupport.writableTarget(
            CompressorByteBufferTestSupport.payload.size * 2,
            direct = true,
        )
        val targetStart = target.position()

        val compressFailure = assertFailsWith<LZ4Exception> {
            compressor.compress(plain, target)
        }
        compressFailure shouldBeSameInstanceAs failure
        plain.position() shouldBeEqualTo 7
        target.position() shouldBeEqualTo targetStart

        val decompressFailure = assertFailsWith<LZ4Exception> {
            compressor.decompress(compressed, target)
        }
        decompressFailure shouldBeSameInstanceAs failure
        compressed.position() shouldBeEqualTo 0
        target.position() shouldBeEqualTo targetStart
    }

    private fun verifyStorageMatrix(
        input: ByteArray,
        expected: ByteArray,
        operation: (ByteBuffer, ByteBuffer) -> Int,
    ) {
        CompressorByteBufferTestSupport.sources(input).forEach { (_, source) ->
            CompressorByteBufferTestSupport.targets(expected.size).forEach { (_, target) ->
                val sourceStart = source.position()
                val sourceLimit = source.limit()
                val sourceOrder = source.order()
                val targetStart = target.position()
                val targetLimit = target.limit()
                val targetOrder = target.order()
                val before = CompressorByteBufferTestSupport.allBytes(target)

                operation(source, target) shouldBeEqualTo expected.size

                source.position() shouldBeEqualTo sourceStart
                source.limit() shouldBeEqualTo sourceLimit
                source.order() shouldBeEqualTo sourceOrder
                CompressorByteBufferTestSupport.assertMark(source, sourceStart)
                target.position() shouldBeEqualTo targetStart + expected.size
                target.limit() shouldBeEqualTo targetLimit
                target.order() shouldBeEqualTo targetOrder
                CompressorByteBufferTestSupport.assertMark(target, targetStart)
                CompressorByteBufferTestSupport.bytes(target, targetStart, expected.size)
                    .shouldContentEqual(expected)
                CompressorByteBufferTestSupport.allBytes(target).copyOfRange(0, targetStart)
                    .shouldContentEqual(before.copyOfRange(0, targetStart))
                CompressorByteBufferTestSupport.allBytes(target).copyOfRange(targetLimit, target.capacity())
                    .shouldContentEqual(before.copyOfRange(targetLimit, target.capacity()))
            }
        }
    }

    private fun returningOperations(
        compressedSize: Int,
        consumedSize: Int = 0,
    ): LZ4BufferOperations = object: LZ4BufferOperations {
        override fun compress(
            source: ByteBuffer,
            sourceOffset: Int,
            sourceLength: Int,
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
        ): Int = compressedSize

        override fun decompress(
            source: ByteBuffer,
            sourceOffset: Int,
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
        ): Int = consumedSize
    }

    private fun throwingOperations(
        compressFailure: LZ4Exception = LZ4Exception("unexpected compression"),
        decompressFailure: LZ4Exception = LZ4Exception("unexpected decompression"),
    ): LZ4BufferOperations = object: LZ4BufferOperations {
        override fun compress(
            source: ByteBuffer,
            sourceOffset: Int,
            sourceLength: Int,
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
        ): Int = throw compressFailure

        override fun decompress(
            source: ByteBuffer,
            sourceOffset: Int,
            target: ByteBuffer,
            targetOffset: Int,
            targetLength: Int,
        ): Int = throw decompressFailure
    }

    private fun intHeader(value: Int): ByteArray =
        ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array()
}
