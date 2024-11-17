package io.bluetape4k.io.compressor

import okio.Buffer
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import java.io.ByteArrayInputStream

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

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        FramedLZ4CompressorOutputStream(output.outputStream()).use { lz4 ->
            lz4.write(plain)
            lz4.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        ByteArrayInputStream(compressed).use { input ->
            FramedLZ4CompressorInputStream(input).use { lz4 ->
                return Buffer().readFrom(lz4).readByteArray()
            }
        }
    }
}
