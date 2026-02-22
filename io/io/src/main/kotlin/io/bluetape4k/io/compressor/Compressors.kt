package io.bluetape4k.io.compressor

import io.bluetape4k.support.unsafeLazy
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
 * 다양한 [Compressor]를 제공합니다.
 */
/**
 * `Compressors` 싱글톤/유틸리티입니다.
 */
object Compressors {

    val ApacheDeflate: ApacheDeflateCompressor by unsafeLazy { ApacheDeflateCompressor() }
    val Deflate: DeflateCompressor by unsafeLazy { DeflateCompressor() }

    val ApacheGZip: ApacheGZipCompressor by unsafeLazy { ApacheGZipCompressor() }
    val GZip: GZipCompressor by unsafeLazy { GZipCompressor() }

    val LZ4: LZ4Compressor by unsafeLazy { LZ4Compressor() }
    val BlockLZ4: BlockLZ4Compressor by unsafeLazy { BlockLZ4Compressor() }
    val FramedLZ4: FramedLZ4Compressor by unsafeLazy { FramedLZ4Compressor() }

    val Snappy: SnappyCompressor by unsafeLazy { SnappyCompressor() }
    val FramedSnappy: FramedSnappyCompressor by unsafeLazy { FramedSnappyCompressor() }

    val ApacheZstd: ApacheZstdCompressor by unsafeLazy { ApacheZstdCompressor() }
    val Zstd: ZstdCompressor by unsafeLazy { ZstdCompressor() }

    val BZip2: BZip2Compressor by unsafeLazy { BZip2Compressor() }

    /**
     * 스트리밍 압축기 모음입니다.
     *
     * stream 래퍼를 제공하지 않는 알고리즘은 one-shot 어댑터를 사용합니다.
     */
    object Streaming {
        val ApacheDeflate: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { DeflateCompressorOutputStream(it) },
                decompressing = { DeflateCompressorInputStream(it) }
            )
        }
        val Deflate: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { DeflaterOutputStream(it) },
                decompressing = { InflaterInputStream(it) }
            )
        }

        val ApacheGZip: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { GzipCompressorOutputStream(it) },
                decompressing = { GzipCompressorInputStream(it) }
            )
        }
        val GZip: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { GZIPOutputStream(it) },
                decompressing = { GZIPInputStream(it) }
            )
        }

        val LZ4: StreamingCompressor by unsafeLazy { StreamingCompressors.from(Compressors.LZ4) }
        val BlockLZ4: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { BlockLZ4CompressorOutputStream(it) },
                decompressing = { BlockLZ4CompressorInputStream(it) }
            )
        }
        val FramedLZ4: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { FramedLZ4CompressorOutputStream(it) },
                decompressing = { FramedLZ4CompressorInputStream(it) }
            )
        }

        val Snappy: StreamingCompressor by unsafeLazy { StreamingCompressors.from(Compressors.Snappy) }
        val FramedSnappy: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { FramedSnappyCompressorOutputStream(it) },
                decompressing = { FramedSnappyCompressorInputStream(it) }
            )
        }

        val ApacheZstd: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { ZstdCompressorOutputStream(it) },
                decompressing = { ZstdCompressorInputStream(it) }
            )
        }
        val Zstd: StreamingCompressor by unsafeLazy { StreamingCompressors.from(Compressors.Zstd) }

        val BZip2: StreamingCompressor by unsafeLazy {
            StreamingCompressors.of(
                compressing = { BZip2CompressorOutputStream(it) },
                decompressing = { BZip2CompressorInputStream(it) }
            )
        }
    }
}
