package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Apache Commons Compress 라이브러리의 [GzipCompressorOutputStream]을 이용한 GZip 압축기
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, World!".toByteArray()
 * val compressed = Compressors.ApacheGZip.compress(data)
 * val restored = Compressors.ApacheGZip.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [GzipCompressorOutputStream]
 * @see [GzipCompressorInputStream]
 */
class ApacheGZipCompressor: AbstractCompressor() {
    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        GzipCompressorOutputStream(output).use { gzip ->
            gzip.write(plain)
            gzip.flush()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray =
        ByteArrayInputStream(compressed).use { input ->
            GzipCompressorInputStream(input).use { gzip ->
                gzip.readBytes()
            }
        }
}
