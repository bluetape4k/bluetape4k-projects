package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.StreamingCompressor
import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.io.okio.byteStringOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import kotlin.random.Random
import kotlin.test.assertFailsWith

class CompressableSinkAndSourceTest: AbstractOkioTest() {

    companion object: KLogging()

    private fun compressors(): List<Compressor> = listOf(
        Compressors.BZip2,
        Compressors.Deflate,
        Compressors.GZip,
        Compressors.LZ4,
        Compressors.Snappy,
        Compressors.Zstd
    )

    private fun streamingCompressors(): List<StreamingCompressor> = listOf(
        Compressors.Streaming.BZip2,
        Compressors.Streaming.Deflate,
        Compressors.Streaming.GZip,
        Compressors.Streaming.LZ4,
        Compressors.Streaming.Snappy,
        Compressors.Streaming.Zstd
    )

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compressa random sentence`(compressor: Compressor) {
        val original = faker.lorem().sentence()
        val data = bufferOf(original)

        val sink = Buffer()
        Compressable.Sinks.compressableSink(sink, compressor).use { compressableSink ->
            // Write data to compressable sink
            compressableSink.write(data, data.size)
            // compressableSink.flush()
        }

        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        val source = bufferOf(decompressableSource)

        // Verify the decompressed data matches the original
        source.readUtf8() shouldBeEqualTo original
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compress a large string`(compressor: Compressor) {
        val original = faker.lorem().paragraph().repeat(1024)
        val data = bufferOf(original)

        val sink = Buffer()
        Compressable.Sinks.compressableSink(sink, compressor).use { compressableSink ->
            // Write data to compressable sink
            compressableSink.write(data, data.size)
            compressableSink.flush()
        }

        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        val source = bufferOf(decompressableSource)

        // Verify the decompressed data matches the original
        source.readUtf8() shouldBeEqualTo original
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compress a byte string`(compressor: Compressor) {
        val original = byteStringOf(Random.nextBytes(1024 * 1024)) // 1MB of random bytes
        val data = bufferOf(original)

        val sink = Buffer()
        Compressable.Sinks.compressableSink(sink, compressor).use { compressableSink ->
            // Write data to compressable sink
            compressableSink.write(data, data.size)
            compressableSink.flush()
        }

        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        val source = bufferOf(decompressableSource)

        // Verify the decompressed data matches the original
        source.readByteString() shouldBeEqualTo original
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compressable sink 는 요청 byteCount 만큼만 소비한다`(compressor: Compressor) {
        val original = "abcdefg"
        val source = bufferOf(original)
        val compressed = Buffer()

        Compressable.Sinks.compressableSink(compressed, compressor).use { sink ->
            sink.write(source, 3L)
        }
        val decoded = bufferOf(Compressable.Sources.decompressableSource(compressed, compressor))
        decoded.readUtf8() shouldBeEqualTo "abc"
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compressable sink 다중 write 를 단일 압축 스트림으로 처리한다`(compressor: Compressor) {
        val source1 = bufferOf("abc")
        val source2 = bufferOf("def")
        val source3 = bufferOf("ghi")
        val compressed = Buffer()

        Compressable.Sinks.compressableSink(compressed, compressor).use { sink ->
            sink.write(source1, source1.size)
            sink.write(source2, source2.size)
            sink.write(source3, source3.size)
        }

        val decoded = bufferOf(Compressable.Sources.decompressableSource(compressed, compressor))
        decoded.readUtf8() shouldBeEqualTo "abcdefghi"
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `decompressable source 는 byteCount 만큼만 반환한다`(compressor: Compressor) {
        val original = faker.lorem().paragraph().repeat(8)
        val originalBytes = original.toByteArray()
        val compressed = Buffer()
        Compressable.Sinks.compressableSink(compressed, compressor).use { sink ->
            sink.write(bufferOf(original), original.length.toLong())
        }

        val source = Compressable.Sources.decompressableSource(compressed, compressor)
        val output = Buffer()
        val firstRead = source.read(output, 10L)
        firstRead shouldBeEqualTo 10L
        output.readByteArray() shouldBeEqualTo originalBytes.copyOfRange(0, firstRead.toInt())
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `compressable sink invalid byteCount behavior`(compressor: Compressor) {
        val source = bufferOf("hello")
        val compressed = Buffer()

        Compressable.Sinks.compressableSink(compressed, compressor).use { sink ->
            sink.write(source, -1L)
            source.size shouldBeEqualTo 5L
            compressed.size shouldBeEqualTo 0L

            assertFailsWith<IllegalArgumentException> {
                sink.write(source, source.size + 1L)
            }
        }
    }

    @ParameterizedTest(name = "compressor={0}")
    @MethodSource("compressors")
    fun `decompressable source throws when delegate repeatedly makes no progress`(compressor: Compressor) {
        val noProgressSource = object: okio.Source {
            override fun read(sink: Buffer, byteCount: Long): Long = 0L
            override fun timeout() = okio.Timeout.NONE
            override fun close() {}
        }
        val source = DecompressableSource(noProgressSource, compressor)

        assertFailsWith<IOException> {
            source.read(Buffer(), 1L)
        }
    }

    @ParameterizedTest(name = "streamingCompressor={0}")
    @MethodSource("streamingCompressors")
    fun `streaming compressor 오버로드를 통해 roundtrip 한다`(compressor: StreamingCompressor) {
        val original = faker.lorem().paragraph().repeat(16)
        val source = bufferOf(original)
        val compressed = Buffer()

        compressed.asCompressSink(compressor).use { sink ->
            sink.write(source, source.size)
        }

        val decompressed = bufferOf(compressed.asDecompressSource(compressor))
        decompressed.readUtf8() shouldBeEqualTo original
    }

    @ParameterizedTest(name = "streamingCompressor={0}")
    @MethodSource("streamingCompressors")
    fun `streaming compressor 오버로드는 다중 write 를 스트리밍으로 처리한다`(compressor: StreamingCompressor) {
        val part1 = faker.lorem().paragraph()
        val part2 = faker.lorem().paragraph()
        val part3 = faker.lorem().paragraph()
        val expected = part1 + part2 + part3
        val compressed = Buffer()

        compressed.asCompressSink(compressor).use { sink ->
            val source1 = bufferOf(part1)
            val source2 = bufferOf(part2)
            val source3 = bufferOf(part3)
            sink.write(source1, source1.size)
            sink.write(source2, source2.size)
            sink.write(source3, source3.size)
        }

        val restored = bufferOf(compressed.asDecompressSource(compressor))
        restored.readUtf8() shouldBeEqualTo expected
    }
}
