package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipException

class DeflateCompressorByteBufferTest {
    private val compressor = DeflateCompressor()

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
    fun `caller-owned APIs create a fresh JDK codec for each operation`() {
        val deflaterCreations = AtomicInteger()
        val inflaterCreations = AtomicInteger()
        val deflaterEnds = AtomicInteger()
        val inflaterEnds = AtomicInteger()
        val compressor = DeflateCompressor.forTesting(
            deflaterFactory = {
                deflaterCreations.incrementAndGet()
                Deflater()
            },
            inflaterFactory = {
                inflaterCreations.incrementAndGet()
                Inflater()
            },
            endDeflater = {
                deflaterEnds.incrementAndGet()
                it.end()
            },
            endInflater = {
                inflaterEnds.incrementAndGet()
                it.end()
            },
        )
        val payload = CompressorByteBufferTestSupport.payload
        val firstTarget = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, direct = true)
        val secondTarget = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, direct = false)

        val firstSize = compressor.compress(CompressorByteBufferTestSupport.direct(payload), firstTarget)
        compressor.compress(CompressorByteBufferTestSupport.heap(payload), secondTarget)

        val firstWire = CompressorByteBufferTestSupport.bytes(firstTarget, 5, firstSize)
        val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        compressor.decompress(CompressorByteBufferTestSupport.direct(firstWire), restored)

        deflaterCreations.get() shouldBeEqualTo 2
        inflaterCreations.get() shouldBeEqualTo 1
        deflaterEnds.get() shouldBeEqualTo 2
        inflaterEnds.get() shouldBeEqualTo 1
    }

    @Test
    fun `caller-owned compression rolls back an exhausted target and permits retry`() {
        val largePayload = ByteArray(16 * 1024) { index ->
            ((index * 31) xor (index ushr 3)).toByte()
        }
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
    fun `data format failures observed before target exhaustion keep their cause`() {
        val payload = "retry after corruption".encodeToByteArray()
        val wire = compressor.compress(payload)
        val cause = DataFormatException("corrupt")
        val cleanupInvocations = AtomicInteger()
        val compressor = DeflateCompressor.forTesting(
            deflaterFactory = ::Deflater,
            inflaterFactory = { ThrowingInflater(cause) },
            endDeflater = Deflater::end,
            endInflater = {
                cleanupInvocations.incrementAndGet()
                it.end()
            },
        )
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        val targetStart = target.position()
        target.limit(targetStart)

        val failure = assertFailsWith<ZipException> {
            compressor.decompress(
                CompressorByteBufferTestSupport.direct(byteArrayOf(1)),
                target,
            )
        }

        failure.message shouldBeEqualTo "Invalid Deflate payload"
        failure.cause shouldBeSameInstanceAs cause
        cleanupInvocations.get() shouldBeEqualTo 1
        target.position() shouldBeEqualTo targetStart
        target.limit() shouldBeEqualTo targetStart

        target.limit(targetStart + payload.size)
        this.compressor.decompress(CompressorByteBufferTestSupport.heap(wire), target)
            .shouldBeEqualTo(payload.size)
        CompressorByteBufferTestSupport.bytes(target, targetStart, payload.size)
            .shouldContentEqual(payload)
    }

    @Test
    fun `truncated payload and preset dictionary states fail with stable ZipExceptions`() {
        val payload = CompressorByteBufferTestSupport.payload
        val wire = compressor.compress(payload)
        val truncated = wire.copyOf(wire.size - 1)

        val truncatedFailure = assertFailsWith<ZipException> {
            compressor.decompress(
                CompressorByteBufferTestSupport.heap(truncated),
                CompressorByteBufferTestSupport.writableTarget(payload.size + 1, direct = true),
            )
        }
        truncatedFailure.message shouldBeEqualTo "Truncated Deflate payload"

        val dictionaryFailure = assertFailsWith<ZipException> {
            compressorWithInflater(DictionaryInflater()).decompress(
                CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(32, direct = false),
            )
        }
        dictionaryFailure.message shouldBeEqualTo "Deflate preset dictionary is required"
    }

    @Test
    fun `target exhaustion wins before dictionary and no-progress states`() {
        val retryPayload = byteArrayOf(1, 2, 3)
        val retryWire = compressor.compress(retryPayload)

        listOf(DictionaryInflater(), NoProgressInflater()).forEach { inflater ->
            val target = CompressorByteBufferTestSupport.writableTarget(retryPayload.size, direct = true)
            val targetStart = target.position()
            target.limit(targetStart)

            assertFailsWith<BufferOverflowException> {
                compressorWithInflater(inflater).decompress(
                    CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                    target,
                )
            }

            target.position() shouldBeEqualTo targetStart
            target.limit() shouldBeEqualTo targetStart

            target.limit(targetStart + retryPayload.size)
            compressor.decompress(CompressorByteBufferTestSupport.heap(retryWire), target)
                .shouldBeEqualTo(retryPayload.size)
            CompressorByteBufferTestSupport.bytes(target, targetStart, retryPayload.size)
                .shouldContentEqual(retryPayload)
        }
    }

    @Test
    fun `codec no-progress states fail closed without looping`() {
        val needsInputFailure = assertFailsWith<IllegalStateException> {
            compressorWithDeflater(NeedsInputDeflater()).compress(
                CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(32, direct = true),
            )
        }
        needsInputFailure.message shouldBeEqualTo "Deflater needs input before finishing"

        val compressionFailure = assertFailsWith<IllegalStateException> {
            compressorWithDeflater(NoProgressDeflater()).compress(
                CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(32, direct = true),
            )
        }
        compressionFailure.message shouldBeEqualTo "Deflater made no progress"

        val decompressionFailure = assertFailsWith<ZipException> {
            compressorWithInflater(NoProgressInflater()).decompress(
                CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(32, direct = true),
            )
        }
        decompressionFailure.message shouldBeEqualTo "Inflater made no progress"
    }

    @Test
    fun `operation failure keeps identity and suppresses cleanup failure`() {
        val operationFailure = CancellationException("operation")
        val cleanupFailure = AssertionError("cleanup")
        val cleanupInvocations = AtomicInteger()
        val compressor = DeflateCompressor.forTesting(
            deflaterFactory = { ThrowingDeflater(operationFailure) },
            inflaterFactory = ::Inflater,
            endDeflater = {
                cleanupInvocations.incrementAndGet()
                it.end()
                throw cleanupFailure
            },
            endInflater = Inflater::end,
        )

        val failure = assertFailsWith<Throwable> {
            compressor.compress(
                CompressorByteBufferTestSupport.heap(byteArrayOf(1)),
                CompressorByteBufferTestSupport.writableTarget(32, direct = true),
            )
        }

        failure shouldBeSameInstanceAs operationFailure
        failure.suppressed.shouldHaveSize(1)
        failure.suppressed.single() shouldBeSameInstanceAs cleanupFailure
        cleanupInvocations.get() shouldBeEqualTo 1
    }

    @Test
    fun `cleanup helper preserves fatal cancellation overflow and runtime operation failures`() {
        val operationFailures = listOf(
            IllegalStateException("runtime"),
            BufferOverflowException(),
            AssertionError("fatal"),
            CancellationException("cancelled"),
        )

        operationFailures.forEachIndexed { index, operationFailure ->
            val cleanupFailure = IllegalArgumentException("cleanup-$index")
            val failure = assertFailsWith<Throwable> {
                useDeflateCodec(Unit, cleanup = { throw cleanupFailure }) {
                    throw operationFailure
                }
            }

            failure shouldBeSameInstanceAs operationFailure
            failure.suppressed.shouldHaveSize(1)
            failure.suppressed.single() shouldBeSameInstanceAs cleanupFailure
        }
    }

    @Test
    fun `cleanup helper neither self-suppresses nor duplicates the same cleanup failure`() {
        val sharedFailure = IllegalStateException("shared")
        val selfSuppressed = assertFailsWith<Throwable> {
            useDeflateCodec(Unit, cleanup = { throw sharedFailure }) {
                throw sharedFailure
            }
        }
        selfSuppressed shouldBeSameInstanceAs sharedFailure
        selfSuppressed.suppressed.shouldHaveSize(0)

        val operationFailure = IllegalStateException("operation")
        val cleanupFailure = IllegalArgumentException("cleanup")
        operationFailure.addSuppressed(cleanupFailure)
        val duplicateSuppressed = assertFailsWith<Throwable> {
            useDeflateCodec(Unit, cleanup = { throw cleanupFailure }) {
                throw operationFailure
            }
        }
        duplicateSuppressed shouldBeSameInstanceAs operationFailure
        duplicateSuppressed.suppressed.shouldHaveSize(1)
        duplicateSuppressed.suppressed.single() shouldBeSameInstanceAs cleanupFailure
    }

    @Test
    fun `cleanup-only failure keeps identity and target rollback`() {
        val cleanupFailure = AssertionError("cleanup")
        val cleanupInvocations = AtomicInteger()
        val target = CompressorByteBufferTestSupport.writableTarget(64, direct = true)
        val targetStart = target.position()
        val compressor = DeflateCompressor.forTesting(
            deflaterFactory = ::Deflater,
            inflaterFactory = ::Inflater,
            endDeflater = {
                cleanupInvocations.incrementAndGet()
                it.end()
                throw cleanupFailure
            },
            endInflater = Inflater::end,
        )

        val failure = assertFailsWith<Throwable> {
            compressor.compress(CompressorByteBufferTestSupport.heap(byteArrayOf(1)), target)
        }

        failure shouldBeSameInstanceAs cleanupFailure
        target.position() shouldBeEqualTo targetStart
        cleanupInvocations.get() shouldBeEqualTo 1
    }

    @Test
    fun `singleton caller-owned operations are safe under concurrency`() {
        val payload = CompressorByteBufferTestSupport.payload

        MultithreadingTester()
            .workers(8)
            .rounds(2)
            .add {
                val wire = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, direct = true)
                val wireStart = wire.position()
                val written = Compressors.Deflate.compress(
                    CompressorByteBufferTestSupport.direct(payload),
                    wire,
                )
                val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = false)
                Compressors.Deflate.decompress(
                    wire.duplicate().position(wireStart).limit(wireStart + written),
                    restored,
                )
                CompressorByteBufferTestSupport.bytes(restored, 5, payload.size)
                    .shouldContentEqual(payload)
            }
            .run()
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

    private fun compressorWithDeflater(deflater: Deflater): DeflateCompressor =
        DeflateCompressor.forTesting(
            deflaterFactory = { deflater },
            inflaterFactory = ::Inflater,
            endDeflater = Deflater::end,
            endInflater = Inflater::end,
        )

    private fun compressorWithInflater(inflater: Inflater): DeflateCompressor =
        DeflateCompressor.forTesting(
            deflaterFactory = ::Deflater,
            inflaterFactory = { inflater },
            endDeflater = Deflater::end,
            endInflater = Inflater::end,
        )

    private class ThrowingDeflater(
        private val failure: Throwable,
    ): Deflater() {
        override fun deflate(output: ByteBuffer): Int = throw failure
    }

    private class NoProgressDeflater: Deflater() {
        override fun deflate(output: ByteBuffer): Int = 0
        override fun finished(): Boolean = false
        override fun needsInput(): Boolean = false
    }

    private class NeedsInputDeflater: Deflater() {
        override fun deflate(output: ByteBuffer): Int = 0
        override fun finished(): Boolean = false
        override fun needsInput(): Boolean = true
    }

    private class ThrowingInflater(
        private val failure: DataFormatException,
    ): Inflater() {
        override fun inflate(output: ByteBuffer): Int = throw failure
    }

    private class DictionaryInflater: Inflater() {
        override fun inflate(output: ByteBuffer): Int = 0
        override fun finished(): Boolean = false
        override fun needsDictionary(): Boolean = true
        override fun needsInput(): Boolean = false
    }

    private class NoProgressInflater: Inflater() {
        override fun inflate(output: ByteBuffer): Int = 0
        override fun finished(): Boolean = false
        override fun needsDictionary(): Boolean = false
        override fun needsInput(): Boolean = false
    }
}
