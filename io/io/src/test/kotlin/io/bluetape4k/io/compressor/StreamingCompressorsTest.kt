package io.bluetape4k.io.compressor

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class StreamingCompressorsTest {

    companion object {
        @JvmStatic
        fun compressors(): List<Arguments> = listOf(
            Arguments.of("ApacheDeflate", Compressors.Streaming.ApacheDeflate),
            Arguments.of("Deflate", Compressors.Streaming.Deflate),
            Arguments.of("ApacheGZip", Compressors.Streaming.ApacheGZip),
            Arguments.of("GZip", Compressors.Streaming.GZip),
            Arguments.of("LZ4", Compressors.Streaming.LZ4),
            Arguments.of("BlockLZ4", Compressors.Streaming.BlockLZ4),
            Arguments.of("FramedLZ4", Compressors.Streaming.FramedLZ4),
            Arguments.of("Snappy", Compressors.Streaming.Snappy),
            Arguments.of("FramedSnappy", Compressors.Streaming.FramedSnappy),
            Arguments.of("ApacheZstd", Compressors.Streaming.ApacheZstd),
            Arguments.of("Zstd", Compressors.Streaming.Zstd),
            Arguments.of("BZip2", Compressors.Streaming.BZip2),
        )
    }

    @DisplayName("StreamingCompressor 바이트 배열 roundtrip")
    @ParameterizedTest(name = "{0}")
    @MethodSource("compressors")
    fun `roundtrip byte array`(name: String, compressor: StreamingCompressor) {
        val plain = Fakers.randomString(4096, 8192).repeat(2).toUtf8Bytes()

        val compressed = compressor.compress(plain)
        val restored = compressor.decompress(compressed)

        compressed.shouldNotBeEmpty()
        restored shouldBeEqualTo plain
    }

    @DisplayName("StreamingCompressor 스트림 roundtrip")
    @ParameterizedTest(name = "{0}")
    @MethodSource("compressors")
    fun `roundtrip streams`(name: String, compressor: StreamingCompressor) {
        val plain = Fakers.randomString(2048, 4096).repeat(3).toUtf8Bytes()

        val compressedOut = ByteArrayOutputStream()
        compressor.compress(ByteArrayInputStream(plain), compressedOut)

        val restoredOut = ByteArrayOutputStream()
        compressor.decompress(ByteArrayInputStream(compressedOut.toByteArray()), restoredOut)

        restoredOut.toByteArray() shouldBeEqualTo plain
    }

    @Test
    fun `OneShot 어댑터는 close 시점에 데이터를 기록한다`() {
        val plain = "one-shot-streaming-adapter".toUtf8Bytes()
        val compressor = StreamingCompressors.from(Compressors.LZ4)
        val compressedOut = ByteArrayOutputStream()

        val compressing = compressor.compressing(compressedOut)
        compressing.write(plain, 0, 5)
        compressing.write(plain, 5, plain.size - 5)

        compressedOut.toByteArray() shouldBeEqualTo emptyByteArray

        compressing.close()
        compressor.decompress(compressedOut.toByteArray()) shouldBeEqualTo plain
    }

    @Test
    fun `OneShot 어댑터 close 는 delegate 를 닫는다`() {
        val compressor = StreamingCompressors.from(Compressors.LZ4)
        val trackable = TrackableOutputStream()
        val compressing = compressor.compressing(trackable)
        compressing.write("close-propagation".toUtf8Bytes())

        trackable.closed shouldBeEqualTo false
        compressing.close()
        trackable.closed shouldBeEqualTo true
    }

    private class TrackableOutputStream: OutputStream() {
        private val delegate = ByteArrayOutputStream()
        var closed: Boolean = false
            private set

        override fun write(b: Int) {
            delegate.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
        }

        override fun close() {
            closed = true
            delegate.close()
        }
    }
}
