package io.bluetape4k.io.compressor

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * BZip2 알고리즘을 사용한 Compressor
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, World!".toByteArray()
 * val compressed = Compressors.BZip2.compress(data)
 * val restored = Compressors.BZip2.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @param bufferSize 내부 버퍼 크기 (기본값: [DEFAULT_BUFFER_SIZE])
 * @see [BZip2CompressorInputStream]
 * @see [BZip2CompressorOutputStream]
 */
class BZip2Compressor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
) : AbstractCompressor() {
    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(bufferSize)
        BZip2CompressorOutputStream(output).use { bzip2 ->
            bzip2.write(plain)
            bzip2.flush()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray =
        ByteArrayInputStream(compressed).use { input ->
            BZip2CompressorInputStream(input).use { bzip2 ->
                bzip2.readBytes()
            }
        }
}
