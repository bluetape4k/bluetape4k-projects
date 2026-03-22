package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.InflaterInputStream

/**
 * 다양한 [Compressor] 구현체를 지연 초기화(lazy)하여 제공하는 싱글톤 레지스트리입니다.
 */
object Compressors {

    val ApacheDeflate: ApacheDeflateCompressor by lazy { ApacheDeflateCompressor() }
    val Deflate: DeflateCompressor by lazy { DeflateCompressor() }

    val ApacheGZip: ApacheGZipCompressor by lazy { ApacheGZipCompressor() }
    val GZip: GZipCompressor by lazy { GZipCompressor() }

    val LZ4: LZ4Compressor by lazy { LZ4Compressor() }
    val BlockLZ4: BlockLZ4Compressor by lazy { BlockLZ4Compressor() }
    val FramedLZ4: FramedLZ4Compressor by lazy { FramedLZ4Compressor() }

    val Snappy: SnappyCompressor by lazy { SnappyCompressor() }
    val FramedSnappy: FramedSnappyCompressor by lazy { FramedSnappyCompressor() }

    val ApacheZstd: ApacheZstdCompressor by lazy { ApacheZstdCompressor() }
    val Zstd: ZstdCompressor by lazy { ZstdCompressor() }

    val BZip2: BZip2Compressor by lazy { BZip2Compressor() }

    val Zip: ZipCompressor by lazy { ZipCompressor() }

    /**
     * 스트리밍 압축기 모음입니다.
     *
     * stream 래퍼를 제공하지 않는 알고리즘은 one-shot 어댑터를 사용합니다.
     */
    object Streaming {
        val ApacheDeflate: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { DeflateCompressorOutputStream(it) },
                decompressing = { DeflateCompressorInputStream(it) }
            )
        }
        val Deflate: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { DeflaterOutputStream(it) },
                decompressing = { InflaterInputStream(it) }
            )
        }

        val ApacheGZip: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { GzipCompressorOutputStream(it) },
                decompressing = { GzipCompressorInputStream(it) }
            )
        }
        val GZip: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { GZIPOutputStream(it) },
                decompressing = { GZIPInputStream(it) }
            )
        }

        val LZ4: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.LZ4) }
        val BlockLZ4: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { BlockLZ4CompressorOutputStream(it) },
                decompressing = { BlockLZ4CompressorInputStream(it) }
            )
        }
        val FramedLZ4: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { FramedLZ4CompressorOutputStream(it) },
                decompressing = { FramedLZ4CompressorInputStream(it) }
            )
        }

        val Snappy: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Snappy) }
        val FramedSnappy: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { FramedSnappyCompressorOutputStream(it) },
                decompressing = { FramedSnappyCompressorInputStream(it) }
            )
        }

        val ApacheZstd: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { ZstdCompressorOutputStream(it) },
                decompressing = { ZstdCompressorInputStream(it) }
            )
        }
        val Zstd: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Zstd) }

        val BZip2: StreamingCompressor by lazy {
            StreamingCompressors.of(
                compressing = { BZip2CompressorOutputStream(it) },
                decompressing = { BZip2CompressorInputStream(it) }
            )
        }

        val Zip: StreamingCompressor by lazy { StreamingCompressors.from(Compressors.Zip) }
    }
}
