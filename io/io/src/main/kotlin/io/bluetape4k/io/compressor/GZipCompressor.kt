package io.bluetape4k.io.compressor

import io.bluetape4k.support.requirePositiveNumber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException

/**
 * Compresses and decompresses byte arrays with the JDK GZip implementation.
 *
 * Decompression is bounded by [maxDecompressedSize] so compressed payloads
 * cannot expand indefinitely in memory. The default limit is 256 MiB.
 *
 * ```kotlin
 * val data = "Hello, GZip!".toByteArray()
 * val compressed = Compressors.GZip.compress(data)
 * val restored = Compressors.GZip.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @property bufferSize GZip stream buffer size.
 * @property maxDecompressedSize Maximum decompressed output size in bytes.
 * @see [GZIPOutputStream]
 * @see [GZIPInputStream]
 * @throws java.io.IOException when GZip stream processing fails.
 * @throws IllegalArgumentException when decompressed output exceeds [maxDecompressedSize].
 * @throws ZipException when the payload is corrupt or not valid GZip data.
 */
class GZipCompressor @JvmOverloads constructor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    val maxDecompressedSize: Int = DEFAULT_MAX_DECOMPRESSED_SIZE,
): AbstractCompressor() {

    companion object {
        const val DEFAULT_MAX_DECOMPRESSED_SIZE: Int = 256 * 1024 * 1024
    }

    init {
        bufferSize.requirePositiveNumber("bufferSize")
        maxDecompressedSize.requirePositiveNumber("maxDecompressedSize")
    }

    /**
     * Compresses [plain] bytes with GZip.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(plain.size)
        GZIPOutputStream(output, bufferSize).use { gzip ->
            gzip.write(plain)
            gzip.finish()
        }
        return output.toByteArray()
    }

    /**
     * Decompresses [compressed] bytes and rejects output above [maxDecompressedSize].
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            GZIPInputStream(input, bufferSize).use { gzip ->
                gzip.readBoundedBytes()
            }
        }
    }

    private fun GZIPInputStream.readBoundedBytes(): ByteArray {
        val output = ByteArrayOutputStream(compressedOutputBufferSize())
        val buffer = ByteArray(bufferSize)

        while (true) {
            val read = read(buffer)
            if (read < 0) {
                return output.toByteArray()
            }
            if (read == 0) {
                continue
            }

            require(read <= maxDecompressedSize - output.size()) {
                "GZip decompressed output exceeds maxDecompressedSize=$maxDecompressedSize bytes."
            }
            output.write(buffer, 0, read)
        }
    }

    private fun compressedOutputBufferSize(): Int =
        minOf(bufferSize, maxDecompressedSize)
}
