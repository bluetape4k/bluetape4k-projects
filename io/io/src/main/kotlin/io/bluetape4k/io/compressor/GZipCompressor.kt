package io.bluetape4k.io.compressor

import io.bluetape4k.io.okio.bufferOf
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

    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        GZIPOutputStream(output.outputStream(), bufferSize).use { gzip ->
            gzip.write(plain)
            gzip.finish()
        }
        return output.readByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            GZIPInputStream(input, bufferSize).use { gzip ->
                bufferOf(gzip).readByteArray()
            }
        }
    }
}
