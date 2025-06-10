package io.bluetape4k.io.compressor

import okio.Buffer
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * JDK GZip 알고리즘을 이용한 압축/복원
 *
 * ```
 * val compressor = GZipCompressor()
 * val compressed = compressor.compress("Hello, GZip!")
 * val decompressed = compressor.decompress(compressed)  // "Hello, GZip!"
 * ```
 *
 * @see [GZIPOutputStream]
 * @see [GZIPInputStream]
 */
class GZipCompressor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
): AbstractCompressor() {

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        GZIPOutputStream(output.outputStream()).use { gzip ->
            gzip.write(plain)
            gzip.finish()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        ByteArrayInputStream(compressed).use { input ->
            GZIPInputStream(input).use { gzip ->
                return Buffer().readFrom(gzip).readByteArray()
            }
        }
    }
}
