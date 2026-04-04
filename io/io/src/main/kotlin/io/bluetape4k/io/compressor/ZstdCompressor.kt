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
 */
class ZstdCompressor private constructor(val level: Int): AbstractCompressor() {

    companion object: KLogging() {
        private const val MAGIC_NUMBER_SIZE = Int.SIZE_BYTES
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
        val sourceSize = plain.size
        val maxOutputSize = Zstd.compressBound(sourceSize.toLong()).toInt()

        val output = ByteArray(MAGIC_NUMBER_SIZE + maxOutputSize)
        sourceSize.toByteArray().copyInto(output, 0)

        val compressedSize = Zstd.compressByteArray(
            output,
            MAGIC_NUMBER_SIZE,
            maxOutputSize,      // output 배열에서 MAGIC_NUMBER_SIZE 이후 사용 가능한 최대 공간
            plain,
            0,
            plain.size,
            level
        )

        return output.copyOf(MAGIC_NUMBER_SIZE + compressedSize.toInt())
    }

    /**
     * I/O 압축에서 `doDecompress` 함수를 제공합니다.
     */
    override fun doDecompress(compressed: ByteArray): ByteArray {
        val sourceSize = compressed.toInt()
        require(sourceSize >= 0) { "sourceSize가 음수입니다. 손상된 데이터일 수 있습니다. sourceSize=$sourceSize" }
        require(sourceSize <= 256 * 1024 * 1024) {
            "sourceSize가 허용 한도(256MB)를 초과합니다. 손상되거나 악의적인 데이터일 수 있습니다. sourceSize=$sourceSize"
        }
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
