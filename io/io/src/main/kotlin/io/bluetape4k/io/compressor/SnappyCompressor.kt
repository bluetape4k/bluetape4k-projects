package io.bluetape4k.io.compressor

import org.xerial.snappy.Snappy

/**
 * Snappy 알고리즘을 사용한 Compressor
 *
 * Snappy 가 Apache Commons Compress 의 FramedSnappyCompressorOutputStream 보다 훨씬 빠르다. (대략 2배 빠르다)
 *
 * ```
 * val compressor = SnappyCompressor()
 * val compressed = compressor.compress("Hello, Snappy!")
 * val decompressed = compressor.decompress(compressed)  // "Hello, Snappy!"
 * ```
 *
 * @see [FramedSnappyCompressor]
 */
class SnappyCompressor: AbstractCompressor() {

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        return Snappy.compress(plain)
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return Snappy.uncompress(compressed)
    }
}
