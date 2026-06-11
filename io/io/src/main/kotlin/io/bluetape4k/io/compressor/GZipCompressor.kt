package io.bluetape4k.io.compressor

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.CRC32
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
        bufferSize.requireGe(1, "bufferSize")
        if (compressionLevel != Deflater.DEFAULT_COMPRESSION) {
            compressionLevel
                .requireGe(Deflater.NO_COMPRESSION, "compressionLevel")
                .requireLe(Deflater.BEST_COMPRESSION, "compressionLevel")
        }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val output = CompressorByteArrayBuffer(plain.size + GZIP_HEADER_SIZE + GZIP_TRAILER_SIZE)
        output.write(GZIP_HEADER)

        val crc = CRC32()
        crc.update(plain)

        val deflater = Deflater(compressionLevel, true)
        try {
            deflater.setInput(plain)
            deflater.finish()

            val buffer = ByteArray(bufferSize)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, length = count)
            }
        } finally {
            deflater.end()
        }

        output.writeIntLe(crc.value.toInt())
        output.writeIntLe(plain.size)
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

    private companion object {
        private const val GZIP_HEADER_SIZE = 10
        private const val GZIP_TRAILER_SIZE = 8

        private val GZIP_HEADER = byteArrayOf(
            0x1f,
            0x8b.toByte(),
            Deflater.DEFLATED.toByte(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
        )
    }
}
