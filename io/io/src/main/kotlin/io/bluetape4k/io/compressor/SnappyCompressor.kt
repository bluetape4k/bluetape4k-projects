package io.bluetape4k.io.compressor

import org.xerial.snappy.Snappy

/**
 * Snappy 알고리즘을 사용한 Compressor
 *
 * Apache Commons Compress의 FramedSnappyCompressorOutputStream보다 훨씬 빠릅니다 (약 2배).
 * 낮은 지연 시간이 중요한 경우에 권장됩니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Snappy!".toByteArray()
 * val compressed = Compressors.Snappy.compress(data)
 * val restored = Compressors.Snappy.decompress(compressed)
 * // restored contentEquals data == true
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
