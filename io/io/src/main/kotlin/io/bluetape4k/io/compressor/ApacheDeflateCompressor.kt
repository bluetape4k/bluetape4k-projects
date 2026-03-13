package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
class ApacheDeflateCompressor : AbstractCompressor() {
    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        DeflateCompressorOutputStream(output).use { deflate ->
            deflate.write(plain)
            deflate.flush()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray =
        ByteArrayInputStream(compressed).use { input ->
            DeflateCompressorInputStream(input).use { deflate ->
                deflate.readBytes()
            }
        }
}
