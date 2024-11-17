package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class CompressorsTest {

    companion object: KLogging()

    @Nested
    @DisplayName("BZip2")
    inner class BZip2CompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = BZip2Compressor()
    }

    @Nested
    @DisplayName("Apache GZip")
    inner class ApacheGZipCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = ApacheGZipCompressor()
    }

    @Nested
    @DisplayName("GZip")
    inner class GZipCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = GZipCompressor()
    }

    @Nested
    @DisplayName("Apache Deflate")
    inner class ApacheDeflaterCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = ApacheDeflateCompressor()
    }

    @Nested
    @DisplayName("Deflate")
    inner class DeflateCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = DeflateCompressor()
    }

    @Nested
    @DisplayName("LZ4")
    inner class LZ4CompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = LZ4Compressor()
    }

    @Nested
    @DisplayName("Block LZ4")
    inner class BlockLZ4CompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = BlockLZ4Compressor()
    }

    @Nested
    @DisplayName("Framed LZ4")
    inner class FramedLZ4CompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = FramedLZ4Compressor()
    }

    @Nested
    @DisplayName("Snappy")
    inner class SnappyCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = SnappyCompressor()
    }

    @Nested
    @DisplayName("FramedSnappy")
    inner class FramedSnappyCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = FramedSnappyCompressor()
    }

    @Nested
    @DisplayName("zstd")
    inner class ZstdCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = ZstdCompressor()
    }

    @Nested
    @DisplayName("ApacheZstd")
    inner class ApacheZstdCompressorTest: AbstractCompressorTest() {
        override val compressor: Compressor = ApacheZstdCompressor()
    }
}
