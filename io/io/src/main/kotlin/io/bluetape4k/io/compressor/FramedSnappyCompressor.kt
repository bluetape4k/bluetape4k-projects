package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Apache Commons Compress의 Framed Snappy 알고리즘을 사용한 Compressor
 *
 * ```
 * val compressor = FramedSnappyCompressor()
 * val compressed = compressor.compress("Hello, FramedSnappy!")
 * val decompressed = compressor.decompress(compressed)
 * ```
 *
 * @see [FramedSnappyCompressorOutputStream]
 * @see [FramedSnappyCompressorInputStream]
 */
class FramedSnappyCompressor: AbstractCompressor() {

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        FramedSnappyCompressorOutputStream(output).use { snappy ->
            snappy.write(plain)
            snappy.flush()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            FramedSnappyCompressorInputStream(input).use { snappy ->
                snappy.readBytes()
            }
        }
    }
}
