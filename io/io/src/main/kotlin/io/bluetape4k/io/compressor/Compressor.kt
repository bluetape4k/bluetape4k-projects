package io.bluetape4k.io.compressor

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.getBytes
import io.bluetape4k.io.toByteArray
import io.bluetape4k.io.toInputStream
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * 데이터를 압축/복원하는 압축기의 최상위 인터페이스
 *
 * ## Null 처리 정책
 * - `compress(null)`: 빈 ByteArray를 반환합니다
 * - `decompress(null)` 또는 `decompress(emptyArray)`: 빈 ByteArray를 반환합니다
 * - 압축/해제 실패 시: 구현체에 따라 예외를 던질 수 있습니다
 *
 * ## 사용 예시
 * ```kotlin
 * val compressor = Compressors.LZ4
 * val compressed = compressor.compress("Hello, World!".toByteArray())
 * val decompressed = compressor.decompress(compressed)
 * ```
 */
interface Compressor {

    /**
     * 데이터를 압축합니다.
     *
     * @param plain 원본 데이터 (null이면 빈 배열 반환)
     * @return 압축된 데이터
     */
    fun compress(plain: ByteArray?): ByteArray

    /**
     * 압축된 데이터를 복원합니다.
     *
     * @param compressed 압축된 데이터 (null 또는 empty이면 빈 배열 반환)
     * @return 복원된 데이터
     */
    fun decompress(compressed: ByteArray?): ByteArray

    /**
     * 데이터를 압축합니다.
     *
     * @param plain 원본 데이터
     * @return 압축된 데이터
     */
    fun compress(plain: String): String =
        compress(plain.toUtf8Bytes()).encodeBase64String()

    /**
     * 압축된 데이터를 복원합니다.
     *
     * @param compressed 압축된 데이터
     * @return 복원된 데이터
     */
    fun decompress(compressed: String): String =
        decompress(compressed.decodeBase64ByteArray()).toUtf8String()

    /**
     * 데이터를 압축합니다.
     *
     * @param plainBuffer 원본 데이터
     * @return 압축된 데이터를 담은 [ByteBuffer]
     */
    fun compress(plainBuffer: ByteBuffer): ByteBuffer =
        ByteBuffer.wrap(compress(plainBuffer.getBytes()))

    /**
     * 압축된 데이터를 복원합니다.
     *
     * @param compressedBuffer 압축된 데이터
     * @return 복원된 데이터를 담은 [ByteBuffer]
     */
    fun decompress(compressedBuffer: ByteBuffer): ByteBuffer =
        ByteBuffer.wrap(decompress(compressedBuffer.getBytes()))

    /**
     * 데이터를 압축합니다.
     *
     * @param plainStream 원본 데이터
     * @return 압축된 데이터를 담은 [InputStream]
     */
    fun compress(plainStream: InputStream): InputStream =
        compress(plainStream.toByteArray()).toInputStream()

    /**
     * 압축된 데이터를 복원합니다.
     *
     * @param compressedStream 압축된 데이터
     * @return 복원된 데이터를 담은 [InputStream]
     */
    fun decompress(compressedStream: InputStream): InputStream =
        decompress(compressedStream.toByteArray()).toInputStream()
}
