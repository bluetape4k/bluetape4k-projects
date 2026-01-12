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
import kotlin.random.Random

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
            compressableSink.flush()
        }

        val source = Buffer()
        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        decompressableSource.read(source, sink.size)

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

        val source = Buffer()
        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        decompressableSource.read(source, sink.size)

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

        val source = Buffer()
        val decompressableSource = Compressable.Sources.decompressableSource(sink, compressor)
        decompressableSource.read(source, sink.size)

        // Verify the decompressed data matches the original
        source.readByteString() shouldBeEqualTo original
    }
}
