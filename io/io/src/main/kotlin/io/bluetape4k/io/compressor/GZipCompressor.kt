package io.bluetape4k.io.compressor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException

/**
 * JDK GZip 알고리즘을 이용한 압축/복원
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, GZip!".toByteArray()
 * val compressed = Compressors.GZip.compress(data)
 * val restored = Compressors.GZip.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [GZIPOutputStream]
 * @see [GZIPInputStream]
 * @throws java.io.IOException when GZip stream processing fails.
 * @throws ZipException when the payload is corrupt or not valid GZip data.
 */
class GZipCompressor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
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
        val gzip = if (compressionLevel == Deflater.DEFAULT_COMPRESSION) {
            GZIPOutputStream(output, bufferSize)
        } else {
            LevelGzipOutputStream(output, bufferSize, compressionLevel)
        }
        gzip.use {
            gzip.write(plain)
            gzip.finish()
        }
        return output.toByteArray()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            GZIPInputStream(input, bufferSize).use { gzip ->
                gzip.readBytes()
            }
        }
    }

    private class LevelGzipOutputStream(
        output: ByteArrayOutputStream,
        size: Int,
        level: Int,
    ): GZIPOutputStream(output, size) {
        init {
            def.setLevel(level)
        }
    }

    private fun isValidCompressionLevel(level: Int): Boolean =
        level == Deflater.DEFAULT_COMPRESSION || level in Deflater.NO_COMPRESSION..Deflater.BEST_COMPRESSION
}
