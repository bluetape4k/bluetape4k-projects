package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
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
}
