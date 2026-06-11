package io.bluetape4k.io.compressor

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireLe
import io.bluetape4k.support.toInt
import org.apache.commons.compress.compressors.zstandard.ZstdUtils
import java.nio.ByteBuffer

/**
 * zstd-jni 라이브러리를 사용하여 Zstd 알고리즘을 활용한 압축기
 *
 * 높은 압축률과 빠른 속도를 동시에 제공하여 대용량 데이터 압축이나 네트워크 전송 최적화에 적합합니다.
 *
 * 팩토리를 통한 사용을 권장합니다:
 * ```kotlin
 * val data = "Hello, Zstd!".toByteArray()
 * val compressed = Compressors.Zstd.compress(data)
 * val restored = Compressors.Zstd.decompress(compressed)
 * // restored contentEquals data == true
 * ```
 *
 * 압축 레벨을 지정하려면 직접 인스턴스화하세요:
 * ```kotlin
 * val compressor = ZstdCompressor(level = 10)
 * ```
 *
 * 참고: [zstd-jni](https://github.com/luben/zstd-jni)
 *
 * @property level 압축 레벨
 * @throws IllegalArgumentException when the stored source-size header is negative or exceeds the 256 MB limit.
 * @throws IllegalStateException when the Zstd compression API returns an error code.
 * @throws ZstdException when zstd-jni codec processing fails.
 */
class ZstdCompressor private constructor(val level: Int): AbstractCompressor() {

    companion object: KLogging() {
        private const val MAGIC_NUMBER_SIZE = Int.SIZE_BYTES
        private const val MAX_DECOMPRESSED_SIZE = 256 * 1024 * 1024
        const val DEFAULT_LEVEL: Int = 3

        /**
         * I/O 압축용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(level: Int = DEFAULT_LEVEL): ZstdCompressor {
            val cLevel = level.coerceIn(Zstd.minCompressionLevel(), Zstd.maxCompressionLevel())
            ZstdUtils.setCacheZstdAvailablity(true)
            return ZstdCompressor(cLevel)
        }
    }

    /**
     * I/O 압축에서 `doCompress` 함수를 제공합니다.
     */
    override fun doCompress(plain: ByteArray): ByteArray {
        val compressed = Zstd.compress(plain, level)
        val output = ByteArray(MAGIC_NUMBER_SIZE + compressed.size)
        val sourceSize = plain.size

        output[0] = (sourceSize ushr 24).toByte()
        output[1] = (sourceSize ushr 16).toByte()
        output[2] = (sourceSize ushr 8).toByte()
        output[3] = sourceSize.toByte()
        compressed.copyInto(output, destinationOffset = MAGIC_NUMBER_SIZE)

        return output
    }

    override fun doCompress(plainBuffer: ByteBuffer): ByteBuffer {
        if (!plainBuffer.isDirect) return super.doCompress(plainBuffer)

        val sourceSize = plainBuffer.remaining()
        val maxOutputSize = Zstd.compressBound(sourceSize.toLong()).toInt()
        val output = ByteBuffer.allocateDirect(MAGIC_NUMBER_SIZE + maxOutputSize)

        output.putInt(sourceSize)
        val compressedSize = Zstd.compressDirectByteBuffer(
            output,
            MAGIC_NUMBER_SIZE,
            maxOutputSize,
            plainBuffer,
            plainBuffer.position(),
            sourceSize,
            level
        )

        check(!Zstd.isError(compressedSize)) { "Zstd compression failed: ${Zstd.getErrorName(compressedSize)}" }
        output.position(0)
        output.limit(MAGIC_NUMBER_SIZE + compressedSize.toInt())
        return output.slice()
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        val sourceSize = compressed.toInt()
            .requireGe(0, "sourceSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "sourceSize")
        val output = ByteArray(sourceSize)

        log.trace { "sourceSize = $sourceSize" }

        Zstd.decompressByteArray(
            output,
            0,
            output.size,
            compressed,
            MAGIC_NUMBER_SIZE,
            compressed.size - MAGIC_NUMBER_SIZE
        )

        return output
    }

    override fun doDecompress(compressedBuffer: ByteBuffer): ByteBuffer {
        if (!compressedBuffer.isDirect) return super.doDecompress(compressedBuffer)

        val sourceSize = compressedBuffer.getInt(compressedBuffer.position())
            .requireGe(0, "sourceSize")
            .requireLe(MAX_DECOMPRESSED_SIZE, "sourceSize")

        val output = ByteBuffer.allocateDirect(sourceSize)
        val decompressedSize = Zstd.decompressDirectByteBuffer(
            output,
            0,
            sourceSize,
            compressedBuffer,
            compressedBuffer.position() + MAGIC_NUMBER_SIZE,
            compressedBuffer.remaining() - MAGIC_NUMBER_SIZE
        )

        check(!Zstd.isError(decompressedSize)) { "Zstd decompression failed: ${Zstd.getErrorName(decompressedSize)}" }
        output.position(0)
        output.limit(decompressedSize.toInt())
        return output.slice()
    }
}
