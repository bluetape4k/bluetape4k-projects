package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.should
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.concurrent.CancellationException
import java.util.stream.Stream

class CompressorByteBufferContractTest {
    private val fallback = ReversingFallbackCompressor()

    @ParameterizedTest(name = "{0} caller-owned buffer matrix")
    @MethodSource("allCompressors")
    fun `all compressors preserve sources and commit bounded targets`(name: String, compressor: Compressor) {
        val payload = CompressorByteBufferTestSupport.payload
        val compressed = compressor.compress(payload)

        verifyOperation("$name-compress", payload, compressed) { source, target ->
            compressor.compress(source, target)
        }
        verifyOperation("$name-decompress", compressed, payload) { source, target ->
            compressor.decompress(source, target)
        }
    }

    @ParameterizedTest(name = "{0} overflow preflight")
    @MethodSource("allCompressors")
    fun `all compressors preserve state on overflow`(name: String, compressor: Compressor) {
        val payload = CompressorByteBufferTestSupport.payload
        val compressed = compressor.compress(payload)
        verifyOverflow("$name-compress", payload) { source, target -> compressor.compress(source, target) }
        verifyOverflow("$name-decompress", compressed) { source, target -> compressor.decompress(source, target) }
    }

    @Test
    fun `read-only target rejection wins before alias and empty checks`() {
        val readOnly = ByteBuffer.allocate(0).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> { fallback.compress(readOnly, readOnly) }
        assertFailsWith<ReadOnlyBufferException> { fallback.decompress(readOnly, readOnly) }
        fallback.compressInvocations.get() shouldBeEqualTo 0
        fallback.decompressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `same writable object is rejected even when empty`() {
        val buffer = ByteBuffer.allocate(0)

        assertFailsWith<IllegalArgumentException> { fallback.compress(buffer, buffer) }
        assertFailsWith<IllegalArgumentException> { fallback.decompress(buffer, buffer) }
    }

    @Test
    fun `overlapping heap ranges are rejected before invoking codec`() {
        val backing = ByteArray(64)
        val source = ByteBuffer.wrap(backing).apply { position(8); limit(40) }
        val partialTarget = ByteBuffer.wrap(backing).apply { position(32); limit(56) }
        val fullTarget = ByteBuffer.wrap(backing).apply { position(8); limit(40) }

        assertFailsWith<IllegalArgumentException> { fallback.compress(source, partialTarget) }
        assertFailsWith<IllegalArgumentException> { fallback.decompress(source, fullTarget) }
        fallback.compressInvocations.get() shouldBeEqualTo 0
        fallback.decompressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `empty non-overlapping source writes zero without invoking codec`() {
        val source = ByteBuffer.allocate(4).apply { position(2); limit(2); mark() }
        val target = CompressorByteBufferTestSupport.writableTarget(8, direct = true)
        val before = CompressorByteBufferTestSupport.allBytes(target)
        val targetStart = target.position()

        fallback.compress(source, target) shouldBeEqualTo 0
        fallback.decompress(source, target) shouldBeEqualTo 0

        source.position() shouldBeEqualTo 2
        target.position() shouldBeEqualTo targetStart
        CompressorByteBufferTestSupport.allBytes(target) shouldContentEqual before
        fallback.compressInvocations.get() shouldBeEqualTo 0
        fallback.decompressInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `pre-created failures preserve identity and positions`() {
        val failures = listOf(
            AssertionError("fatal"),
            CancellationException("cancelled"),
            IllegalStateException("runtime"),
        )
        failures.forEach { expected ->
            val compressor = throwingCompressor(expected)
            val source = CompressorByteBufferTestSupport.direct(byteArrayOf(1, 2, 3))
            val target = CompressorByteBufferTestSupport.writableTarget(32, direct = true)
            val sourceStart = source.position()
            val targetStart = target.position()

            val compressFailure = assertFailsWith<Throwable> { compressor.compress(source, target) }
            compressFailure shouldBeSameInstanceAs expected
            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart

            val decompressFailure = assertFailsWith<Throwable> { compressor.decompress(source, target) }
            decompressFailure shouldBeSameInstanceAs expected
            source.position() shouldBeEqualTo sourceStart
            target.position() shouldBeEqualTo targetStart
        }
    }

    @Test
    fun `overflow and corruption leave target reusable for retry`() {
        val payload = CompressorByteBufferTestSupport.payload
        val source = CompressorByteBufferTestSupport.heap(payload)
        val tooSmall = CompressorByteBufferTestSupport.writableTarget(payload.size - 1, direct = false)

        assertFailsWith<BufferOverflowException> { fallback.compress(source, tooSmall) }
        source.position() shouldBeEqualTo 7
        tooSmall.position() shouldBeEqualTo 5

        val smallerPayload = payload.copyOf(payload.size - 2)
        val retrySource = CompressorByteBufferTestSupport.heap(smallerPayload)
        val retryStart = tooSmall.position()
        fallback.compress(retrySource, tooSmall) shouldBeEqualTo smallerPayload.size
        CompressorByteBufferTestSupport.bytes(tooSmall, retryStart, smallerPayload.size) shouldContentEqual
                smallerPayload.reversedArray()

        var corrupt = true
        val retrying = object: Compressor {
            override fun compress(plain: ByteArray?): ByteArray = plain ?: ByteArray(0)
            override fun decompress(compressed: ByteArray?): ByteArray {
                if (corrupt) throw IllegalArgumentException("corrupt")
                return compressed ?: ByteArray(0)
            }
        }
        val wire = CompressorByteBufferTestSupport.heap(payload)
        val reused = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        val reusedStart = reused.position()
        assertFailsWith<IllegalArgumentException> { retrying.decompress(wire, reused) }
        reused.position() shouldBeEqualTo reusedStart
        corrupt = false
        retrying.decompress(wire, reused) shouldBeEqualTo payload.size
    }

    @Test
    fun `highly compressible fallback output is capacity checked after transformation`() {
        val payload = ByteArray(16 * 1024)
        val source = CompressorByteBufferTestSupport.heap(payload)
        val tiny = CompressorByteBufferTestSupport.writableTarget(1, direct = false)

        assertFailsWith<BufferOverflowException> { fallback.decompress(source, tiny) }

        fallback.decompressInvocations.get() shouldBeEqualTo 1
        source.position() shouldBeEqualTo 7
        tiny.position() shouldBeEqualTo 5
    }

    @Test
    fun `legacy allocating and caller-owned APIs exchange wire formats both ways`() {
        val payload = CompressorByteBufferTestSupport.payload
        allCompressors().forEach { arguments ->
            val compressor = arguments.get()[1] as Compressor

            val legacyWire = compressor.compress(payload)
            val newTarget = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
            compressor.decompress(CompressorByteBufferTestSupport.direct(legacyWire), newTarget) shouldBeEqualTo payload.size
            CompressorByteBufferTestSupport.bytes(newTarget, 5, payload.size) shouldContentEqual payload

            val wireTarget = CompressorByteBufferTestSupport.writableTarget(legacyWire.size, direct = true)
            compressor.compress(CompressorByteBufferTestSupport.heap(payload), wireTarget) shouldBeEqualTo legacyWire.size
            val newWire = CompressorByteBufferTestSupport.bytes(wireTarget, 5, legacyWire.size)
            compressor.decompress(newWire) shouldContentEqual payload
        }
    }

    private fun verifyOperation(
        label: String,
        input: ByteArray,
        expected: ByteArray,
        operation: (ByteBuffer, ByteBuffer) -> Int,
    ) {
        CompressorByteBufferTestSupport.sources(input).forEach { (sourceName, source) ->
            CompressorByteBufferTestSupport.targets(expected.size).forEach { (targetName, target) ->
                val case = "$label-$sourceName-$targetName"
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
                    .shouldContentEqual(expected, case)
                CompressorByteBufferTestSupport.allBytes(target).copyOfRange(0, targetStart)
                    .shouldContentEqual(before.copyOfRange(0, targetStart), "$case prefix")
                CompressorByteBufferTestSupport.allBytes(target).copyOfRange(targetLimit, target.capacity())
                    .shouldContentEqual(before.copyOfRange(targetLimit, target.capacity()), "$case suffix")
            }
        }
    }

    private fun verifyOverflow(
        label: String,
        input: ByteArray,
        operation: (ByteBuffer, ByteBuffer) -> Int,
    ) {
        CompressorByteBufferTestSupport.sources(input).forEach { (sourceName, source) ->
            CompressorByteBufferTestSupport.targets(1).forEach { (targetName, target) ->
                val case = "$label-$sourceName-$targetName"
                val sourceStart = source.position()
                val sourceLimit = source.limit()
                val targetStart = target.position()
                val targetLimit = target.limit()
                val before = CompressorByteBufferTestSupport.allBytes(target)

                assertFailsWith<BufferOverflowException>(case) { operation(source, target) }

                source.position() shouldBeEqualTo sourceStart
                source.limit() shouldBeEqualTo sourceLimit
                target.position() shouldBeEqualTo targetStart
                target.limit() shouldBeEqualTo targetLimit
                CompressorByteBufferTestSupport.assertMark(source, sourceStart)
                CompressorByteBufferTestSupport.assertMark(target, targetStart)
                val after = CompressorByteBufferTestSupport.allBytes(target)
                after.copyOfRange(0, targetStart)
                    .shouldContentEqual(before.copyOfRange(0, targetStart), "$case prefix")
                after.copyOfRange(targetLimit, target.capacity())
                    .shouldContentEqual(before.copyOfRange(targetLimit, target.capacity()), "$case suffix")
            }
        }
    }

    private fun ByteArray.shouldContentEqual(expected: ByteArray, case: String) {
        should("$case: expected byte content equality") { actual -> actual.contentEquals(expected) }
    }

    private fun throwingCompressor(failure: Throwable): Compressor = object: Compressor {
        override fun compress(plain: ByteArray?): ByteArray = throw failure
        override fun decompress(compressed: ByteArray?): ByteArray = throw failure
    }

    companion object {
        @JvmStatic
        fun allCompressors(): Stream<Arguments> = Stream.of(
            Arguments.of("apache-deflate", Compressors.ApacheDeflate),
            Arguments.of("deflate", Compressors.Deflate),
            Arguments.of("apache-gzip", Compressors.ApacheGZip),
            Arguments.of("gzip", Compressors.GZip),
            Arguments.of("lz4", Compressors.LZ4),
            Arguments.of("block-lz4", Compressors.BlockLZ4),
            Arguments.of("framed-lz4", Compressors.FramedLZ4),
            Arguments.of("snappy", Compressors.Snappy),
            Arguments.of("framed-snappy", Compressors.FramedSnappy),
            Arguments.of("apache-zstd", Compressors.ApacheZstd),
            Arguments.of("zstd", Compressors.Zstd),
            Arguments.of("bzip2", Compressors.BZip2),
            Arguments.of("zip", Compressors.Zip),
            Arguments.of("test-fallback", ReversingFallbackCompressor()),
        )
    }
}
