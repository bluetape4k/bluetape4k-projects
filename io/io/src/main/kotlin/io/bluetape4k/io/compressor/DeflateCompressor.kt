package io.bluetape4k.io.compressor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import java.util.zip.ZipException

/**
 * JDK Deflate 알고리즘을 이용한 압축기
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Deflate!".toByteArray()
 * val compressed = Compressors.Deflate.compress(data)
 * val restored = Compressors.Deflate.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [DeflaterOutputStream]
 * @see [InflaterInputStream]
 * @throws java.io.IOException when Deflate stream processing fails.
 * @throws ZipException when the payload is corrupt or not valid Deflate data.
 */
class DeflateCompressor(
    private val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
): AbstractCompressor() {

    init {
        require(bufferSize > 0) { "bufferSize must be greater than 0." }
        require(isValidCompressionLevel(compressionLevel)) {
            "compressionLevel must be ${Deflater.DEFAULT_COMPRESSION} or between " +
                    "${Deflater.NO_COMPRESSION} and ${Deflater.BEST_COMPRESSION}."
        }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        val deflater = Deflater(compressionLevel)
        try {
            DeflaterOutputStream(output, deflater, bufferSize).use { deflate ->
                deflate.write(plain)
                deflate.finish()
            }
        } finally {
            deflater.end()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            InflaterInputStream(input).use { inflate ->
                inflate.readBytes()
            }
        }
    }

    private fun isValidCompressionLevel(level: Int): Boolean =
        level == Deflater.DEFAULT_COMPRESSION || level in Deflater.NO_COMPRESSION..Deflater.BEST_COMPRESSION
}
