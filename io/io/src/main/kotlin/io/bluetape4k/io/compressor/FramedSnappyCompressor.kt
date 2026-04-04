package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Apache Commons Compress의 Framed Snappy 알고리즘을 사용한 Compressor
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, FramedSnappy!".toByteArray()
 * val compressed = Compressors.FramedSnappy.compress(data)
 * val restored = Compressors.FramedSnappy.decompress(compressed)
 * // restored contentEquals data == true
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
