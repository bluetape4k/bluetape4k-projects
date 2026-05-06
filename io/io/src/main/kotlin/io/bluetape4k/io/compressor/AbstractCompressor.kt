package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty

/**
 * [Compressor]의 최상위 추상화 클래스입니다.
 *
 * ## Null/Empty 처리 정책
 * - `compress(null)` 또는 `compress(emptyByteArray)`: 빈 [ByteArray]를 반환합니다.
 * - `decompress(null)` 또는 `decompress(emptyByteArray)`: 빈 [ByteArray]를 반환합니다.
 * - 압축/해제 실패 시 예외를 그대로 전파합니다.
 *
 * ## 예외 처리
 * `compress()`/`decompress()` 는 내부 예외를 호출자에게 전파합니다.
 * 실패 시 `null`을 반환받으려면 [compressOrNull] / [decompressOrNull] 을 사용하세요.
 *
 * ## 구현 방법
 * [doCompress]와 [doDecompress]를 구현하면 됩니다.
 * null/empty 체크는 이 클래스에서 담당합니다.
 *
 * ```kotlin
 * class MyCompressor: AbstractCompressor() {
 *     override fun doCompress(plain: ByteArray): ByteArray = /* 압축 구현 */
 *     override fun doDecompress(compressed: ByteArray): ByteArray = /* 해제 구현 */
 * }
 * ```
 *
 * @see Compressor
 * @see Compressors
 */
abstract class AbstractCompressor: Compressor {

    companion object: KLogging()

    protected abstract fun doCompress(plain: ByteArray): ByteArray
    protected abstract fun doDecompress(compressed: ByteArray): ByteArray

    /**
     * [plain] 데이터를 압축합니다.
     *
     * null/empty 입력은 빈 [ByteArray]를 반환합니다. 압축 실패 시 예외를 전파합니다.
     * 실패 시 `null`을 받으려면 [compressOrNull]을 사용하세요.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * ```
     *
     * @param plain 원본 데이터
     * @return 압축된 데이터
     * @throws Exception 압축 실패 시
     */
    override fun compress(plain: ByteArray?): ByteArray {
        if (plain.isNullOrEmpty()) return emptyByteArray
        return doCompress(plain!!)
    }

    /**
     * 압축된 데이터([compressed])를 복원하여 [ByteArray]로 반환합니다.
     *
     * null/empty 입력은 빈 [ByteArray]를 반환합니다. 복원 실패 시 예외를 전파합니다.
     * 손상 데이터를 `null`로 처리하려면 [decompressOrNull]을 사용하세요.
     *
     * ```
     * val compressor = GzipCompressor()
     * val compressed = compressor.compress("Hello, World!".toByteArray())
     * val plain = compressor.decompress(compressed)
     * ```
     *
     * @param compressed 압축된 데이터
     * @return 복원된 데이터
     * @throws Exception 복원 실패 시 (손상 데이터, 크기 제한 초과 등)
     */
    override fun decompress(compressed: ByteArray?): ByteArray {
        if (compressed.isNullOrEmpty()) return emptyByteArray
        return doDecompress(compressed!!)
    }

    /**
     * [plain] 데이터를 압축하여 반환합니다. 입력이 null/empty이거나 압축 실패 시 `null`을 반환합니다.
     *
     * `compress()`와 달리 실패를 `null`로 명시적으로 표현하므로, 데이터 손실 없이 오류를 처리할 수 있습니다.
     *
     * ```kotlin
     * val result = compressor.compressOrNull(corruptData)
     * if (result == null) {
     *     // 압축 실패 처리
     * }
     * ```
     *
     * @param plain 원본 데이터
     * @return 압축된 데이터 또는 `null`
     */
    fun compressOrNull(plain: ByteArray?): ByteArray? {
        if (plain.isNullOrEmpty()) return null
        return try {
            doCompress(plain!!)
        } catch (e: Exception) {
            log.error(e) { "Fail to compress. plain size=${plain?.size}" }
            null
        }
    }

    /**
     * 압축된 데이터([compressed])를 복원하여 반환합니다. 입력이 null/empty이거나 복원 실패 시 `null`을 반환합니다.
     *
     * `decompress()`와 달리 실패를 `null`로 명시적으로 표현하므로, 손상 데이터를 빈 배열과 구분할 수 있습니다.
     *
     * ```kotlin
     * val result = compressor.decompressOrNull(suspectData)
     * if (result == null) {
     *     // 손상 데이터 처리
     * }
     * ```
     *
     * @param compressed 압축된 데이터
     * @return 복원된 데이터 또는 `null`
     */
    fun decompressOrNull(compressed: ByteArray?): ByteArray? {
        if (compressed.isNullOrEmpty()) return null
        return try {
            doDecompress(compressed!!)
        } catch (e: Exception) {
            log.error(e) { "Fail to decompress. compressed size=${compressed?.size}" }
            null
        }
    }
}
