package io.bluetape4k.io.compressor

import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import java.io.ByteArrayInputStream

/**
 * Apache Commons Compress 라이브러리의 [DeflateCompressorOutputStream]을 이용한 Deflate 압축기
 *
 * ```
 * val compressor = ApacheDeflateCompressor()
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val plain = compressor.decompress(compressed)
 * ```
 *
 * @see [DeflateCompressorOutputStream]
 * @see [DeflateCompressorInputStream]
 */
class ApacheDeflateCompressor: AbstractCompressor() {

    companion object: KLogging()

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        DeflateCompressorOutputStream(output.outputStream()).use { deflate ->
            deflate.write(plain)
            deflate.flush()
        }
        return output.readByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            DeflateCompressorInputStream(input).use { deflate ->
                bufferOf(deflate).readByteArray()
            }
        }
    }
}
