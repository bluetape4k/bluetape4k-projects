package io.bluetape4k.io.compressor

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import org.xerial.snappy.Snappy
import org.xerial.snappy.SnappyError
import java.nio.ByteBuffer

/**
 * Snappy 알고리즘을 사용한 Compressor
 *
 * Apache Commons Compress의 FramedSnappyCompressorOutputStream보다 훨씬 빠릅니다 (약 2배).
 * 낮은 지연 시간이 중요한 경우에 권장됩니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Snappy!".toByteArray()
 * val compressed = Compressors.Snappy.compress(data)
 * val restored = Compressors.Snappy.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * @see [FramedSnappyCompressor]
 * @throws IllegalArgumentException when source-size validation fails or exceeds the 256 MB limit.
 * @throws SnappyError when the Snappy native library or codec reports a state error.
 * @throws org.xerial.snappy.SnappyException when the Snappy codec reports a codec failure.
 */
class SnappyCompressor: AbstractCompressor() {

    companion object {
        private const val MAX_DECOMPRESSED_SIZE = 256 * 1024 * 1024  // 256 MB
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        return Snappy.compress(plain)
    }

    override fun doCompress(plainBuffer: ByteBuffer): ByteBuffer {
        if (!plainBuffer.isDirect) return super.doCompress(plainBuffer)

        val input = plainBuffer.slice()
        val output = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(input.remaining()))

        Snappy.compress(input, output)
        return output.slice()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     *
     * 압축 해제 전에 원본 크기를 확인하여 과도한 메모리 할당을 방지합니다 (decompression bomb 방어).
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        val uncompressedSize = Snappy.uncompressedLength(compressed)
            .requireGe(0, "uncompressedSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "uncompressedSize")
        return Snappy.uncompress(compressed)
    }

    override fun doDecompress(compressedBuffer: ByteBuffer): ByteBuffer {
        if (!compressedBuffer.isDirect) return super.doDecompress(compressedBuffer)

        val input = compressedBuffer.slice()
        val uncompressedSize = Snappy.uncompressedLength(input.duplicate())
            .requireGe(0, "uncompressedSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "uncompressedSize")

        val output = ByteBuffer.allocateDirect(uncompressedSize)

        Snappy.uncompress(input, output)
        return output.slice()
    }
}
