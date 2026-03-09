package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Apache Commons Compress FramedLZ4 알고리즘을 이용한 압축기
 *
 * ```
 * val compressor = FramedLZ4Compressor()
 * val compressed = compressor.compress("Hello, FramedLZ4!")
 * val decompressed = compressor.decompress(compressed)
 * ```
 *
 * @see [FramedLZ4CompressorInputStream]
 * @see [FramedLZ4CompressorOutputStream]
 */
class FramedLZ4Compressor: AbstractCompressor() {

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        FramedLZ4CompressorOutputStream(output).use { lz4 ->
            lz4.write(plain)
            lz4.flush()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            FramedLZ4CompressorInputStream(input).use { lz4 ->
                lz4.readBytes()
            }
        }
    }
}
