package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.ByteArrayInputStream

/**
 * BZip2 알고리즘을 사용한 Compressor
 *
 * ```
 * val compressor = BZip2Compressor()
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val plain = compressor.decompress(compressed)
 * ```
 *
 * @see [BZip2CompressorInputStream]
 * @see [BZip2CompressorOutputStream]
 */
class BZip2Compressor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
): AbstractCompressor() {

    companion object: KLogging()

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        BZip2CompressorOutputStream(output.outputStream()).use { bzip2 ->
            bzip2.write(plain)
            bzip2.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        ByteArrayInputStream(compressed).use { input ->
            BZip2CompressorInputStream(input).use { bzip2 ->
                return Buffer().readFrom(bzip2).readByteArray()
            }
        }
    }
}
