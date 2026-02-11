package io.bluetape4k.io.compressor

import com.github.luben.zstd.Zstd
import io.bluetape4k.io.compressor.ApacheZstdCompressor.Companion.DEFAULT_LEVEL
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdUtils
import java.io.ByteArrayInputStream

/**
 * Apache Compress 라이브러리의 Zstd 알고리즘을 사용한 Compressor (내부적으로 zstd-jni 라이브러리 사용)
 *
 * 참고: [zstd-jni](https://github.com/luben/zstd-jni)
 *
 * ```
 * val compressor = ApacheZstdCompressor()
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val plain = compressor.decompress(compressed)
 * ```
 *
 * @property level 압축 레벨 (기본값: [DEFAULT_LEVEL])
 *
 * @see [ZstdCompressorInputStream]
 * @see [ZstdCompressorOutputStream]
 */
class ApacheZstdCompressor private constructor(val level: Int): AbstractCompressor() {

    companion object: KLogging() {
        const val DEFAULT_LEVEL: Int = 3

        @JvmStatic
        operator fun invoke(level: Int = DEFAULT_LEVEL): ApacheZstdCompressor {
            ZstdUtils.setCacheZstdAvailablity(true)
            val cLevel = level.coerceIn(Zstd.minCompressionLevel(), Zstd.maxCompressionLevel())
            return ApacheZstdCompressor(cLevel)
        }
    }

    override fun doCompress(plain: ByteArray): ByteArray {
        val output = Buffer()
        ZstdCompressorOutputStream(
            ZstdCompressorOutputStream.builder()
                .setOutputStream(output.outputStream())
                .setLevel(level)
                .outputStream
        ).use { zstd ->
            zstd.write(plain)
            zstd.flush()
        }
        return output.readByteArray()
    }

    override fun doDecompress(compressed: ByteArray): ByteArray {
        return ByteArrayInputStream(compressed).use { input ->
            ZstdCompressorInputStream(input).use { zstd ->
                bufferOf(zstd).readByteArray()
            }
        }
    }
}
