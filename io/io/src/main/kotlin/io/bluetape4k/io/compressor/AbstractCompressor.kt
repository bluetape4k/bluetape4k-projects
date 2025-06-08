package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty


/**
 * [Compressor]의 최상위 추상화 클래스입니다.
 */
abstract class AbstractCompressor: Compressor {

    companion object: KLogging()

    protected abstract fun doCompress(plain: ByteArray): ByteArray
    protected abstract fun doDecompress(compressed: ByteArray): ByteArray

    /**
     * [plain] 데이터를 압축합니다.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * ```
     *
     * @param plain 원본 데이터
     * @return 압축된 데이터
     */
    override fun compress(plain: ByteArray?): ByteArray {
        if (plain.isNullOrEmpty()) {
            return emptyByteArray
        }
        return try {
            doCompress(plain!!)
        } catch (e: Throwable) {
            log.warn(e) { "Fail to compress." }
            emptyByteArray
        }
    }

    /**
     * 압축된 데이터([compressed])를 복원하여 [ByteArray]로 반환합니다.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * val plain = compressor.decompress(compressed)
     * ```
     *
     * @param compressed 압축된 데이터
     * @return 복원된 데이터 (압축되지 않은 원본 데이터)
     */
    override fun decompress(compressed: ByteArray?): ByteArray {
        if (compressed.isNullOrEmpty()) {
            return emptyByteArray
        }
        return try {
            doDecompress(compressed!!)
        } catch (e: Throwable) {
            log.warn(e) { "Fail to decompress. compressed size=${compressed?.size}" }
            emptyByteArray
        }
    }
}
