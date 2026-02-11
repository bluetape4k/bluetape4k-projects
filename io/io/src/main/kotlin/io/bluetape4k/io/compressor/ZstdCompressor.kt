package io.bluetape4k.io.compressor

import com.github.luben.zstd.Zstd
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.toByteArray
import io.bluetape4k.support.toInt
import org.apache.commons.compress.compressors.zstandard.ZstdUtils

/**
 * zstd-jni 라이브러리를 사용하여 Zstd 알고리즘을 활용한 압축기
 *
 * 참고: [zstd-jni](https://github.com/luben/zstd-jni)
 *
 * ```
 * val compressor = ZstdCompressor()
 * val compressed = compressor.compress("Hello, Zstd!")
 * val decompressed = compressor.decompress(compressed)  // "Hello, Zstd!"
 * ```
 *
 * @property level 압축 레벨
 */
class ZstdCompressor private constructor(val level: Int): AbstractCompressor() {

    companion object: KLogging() {
        private const val MAGIC_NUMBER_SIZE = Int.SIZE_BYTES
        const val DEFAULT_LEVEL: Int = 3

        @JvmStatic
        operator fun invoke(level: Int = DEFAULT_LEVEL): ZstdCompressor {
            val cLevel = level.coerceIn(Zstd.minCompressionLevel(), Zstd.maxCompressionLevel())
            ZstdUtils.setCacheZstdAvailablity(true)
            return ZstdCompressor(cLevel)
        }
    }

    override fun doCompress(plain: ByteArray): ByteArray {
        val sourceSize = plain.size
        val maxOutputSize = Zstd.compressBound(sourceSize.toLong()).toInt()

        val output = ByteArray(MAGIC_NUMBER_SIZE + maxOutputSize)
        sourceSize.toByteArray().copyInto(output, 0)

        val compressedSize = Zstd.compressByteArray(
            output,
            MAGIC_NUMBER_SIZE,
            maxOutputSize - MAGIC_NUMBER_SIZE,
            plain,
            0,
            plain.size,
            level
        )

        return output.copyOf(MAGIC_NUMBER_SIZE + compressedSize.toInt())
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        val sourceSize = compressed.toInt()
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
}
