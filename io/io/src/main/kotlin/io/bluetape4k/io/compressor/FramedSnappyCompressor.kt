package io.bluetape4k.io.compressor

import io.bluetape4k.io.okio.bufferOf
import okio.Buffer
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream
import java.io.ByteArrayInputStream

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

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        FramedSnappyCompressorOutputStream(output.outputStream()).use { snappy ->
            snappy.write(plain)
            snappy.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            FramedSnappyCompressorInputStream(input).use { snappy ->
                bufferOf(snappy).readByteArray()
            }
        }
    }
}
