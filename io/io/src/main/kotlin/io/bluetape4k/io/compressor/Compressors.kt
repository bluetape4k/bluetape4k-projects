package io.bluetape4k.io.compressor

import io.bluetape4k.support.unsafeLazy

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
}
