package io.bluetape4k.io.compressor

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import java.io.ByteArrayInputStream
import java.util.zip.Deflater
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
 * @see [InflaterInputStream]
 * @throws java.io.IOException when Deflate stream processing fails.
 * @throws ZipException when the payload is corrupt or not valid Deflate data.
 */
class DeflateCompressor(
    private val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
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
        val deflater = Deflater(compressionLevel)
        try {
            deflater.setInput(plain)
            deflater.finish()

            val output = CompressorByteArrayBuffer(plain.size)
            val buffer = ByteArray(bufferSize)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, length = count)
            }
            return output.toByteArray()
        } finally {
            deflater.end()
        }
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
}
