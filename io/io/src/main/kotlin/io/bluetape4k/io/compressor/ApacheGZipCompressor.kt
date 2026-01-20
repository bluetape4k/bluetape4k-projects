package io.bluetape4k.io.compressor

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.ByteArrayInputStream

/**
 * Apache Commons Compress 라이브러리의 [GzipCompressorOutputStream]을 이용한 GZip 압축기
 *
 * ```
 * val compressor = ApacheGZipCompressor()
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val plain = compressor.decompress(compressed)
 * ```
 *
 * @see [GzipCompressorOutputStream]
 * @see [GzipCompressorInputStream]
 */
class ApacheGZipCompressor: AbstractCompressor() {

    companion object: KLogging()

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        GzipCompressorOutputStream(output.outputStream()).use { gzip ->
            gzip.write(plain)
            gzip.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            GzipCompressorInputStream(input).use { gzip ->
                bufferOf(gzip).readByteArray()
            }
        }
    }
}
