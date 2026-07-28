package io.bluetape4k.io.compressor

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.getBytes
import io.bluetape4k.io.toByteArray
import io.bluetape4k.io.toInputStream
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import java.io.InputStream
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

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
     * [source]의 남은 데이터를 호출자가 소유한 [target]에 압축합니다.
     *
     * source의 position, limit, mark, byte order는 보존됩니다. target의 limit,
     * capacity, mark, byte order도 보존됩니다. 성공하면 target의 position만 반환된
     * 바이트 수만큼 이동합니다. 실패하면 target의 position은 복구되지만, 이미 덮어쓴
     * 바이트의 내용은 보장하지 않습니다.
     *
     * 기본 구현은 중간 배열을 할당하는 호환 경로입니다. 동일한 버퍼와 탐지 가능한
     * 쓰기 가능 heap 배열의 중첩 구간은 거부합니다. direct 또는 read-only view를 통한
     * alias는 탐지할 수 없으므로 호출자가 제외해야 합니다. 호출 중에는 각 가변 버퍼를
     * 하나의 thread에서만 사용해야 합니다.
     *
     * 이 함수는 runtime dispatch telemetry나 log를 출력하지 않습니다. 진단이 필요한
     * 호출자는 privacy-safe codec, storage, size metadata를 기록해야 합니다.
     *
     * @return [target]에 기록한 바이트 수
     * @throws ReadOnlyBufferException [target]이 read-only인 경우
     * @throws IllegalArgumentException 탐지 가능한 source와 target 구간이 중첩되는 경우
     * @throws BufferOverflowException [target]의 남은 용량이 부족한 경우
     */
    fun compress(source: ByteBuffer, target: ByteBuffer): Int =
        writeFallback(source, target) { bytes -> compress(bytes) }

    /**
     * [source]의 남은 데이터를 호출자가 소유한 [target]에 해제합니다.
     *
     * source의 position, limit, mark, byte order는 보존됩니다. target의 limit,
     * capacity, mark, byte order도 보존됩니다. 성공하면 target의 position만 반환된
     * 바이트 수만큼 이동합니다. 실패하면 target의 position은 복구되지만, 이미 덮어쓴
     * 바이트의 내용은 보장하지 않습니다.
     *
     * 기본 구현은 중간 배열을 할당하는 호환 경로입니다. target은 최종 쓰기 범위만
     * 제한하며, 신뢰할 수 없는 입력의 압축 해제 resource 사용량을 제한하지 않습니다.
     * 호출자는 application 수준에서 해제 결과의 크기 제한을 적용해야 합니다. 동일한
     * 버퍼와 탐지 가능한 쓰기 가능 heap 배열의 중첩 구간은 거부합니다. direct 또는
     * read-only view를 통한 alias는 탐지할 수 없으므로 호출자가 제외해야 합니다.
     * 호출 중에는 각 가변 버퍼를 하나의 thread에서만 사용해야 합니다.
     *
     * 이 함수는 runtime dispatch telemetry나 log를 출력하지 않습니다. 진단이 필요한
     * 호출자는 privacy-safe codec, storage, size metadata를 기록해야 합니다.
     *
     * @return [target]에 기록한 바이트 수
     * @throws ReadOnlyBufferException [target]이 read-only인 경우
     * @throws IllegalArgumentException 탐지 가능한 source와 target 구간이 중첩되는 경우
     * @throws BufferOverflowException 해제 결과가 [target]에 들어가지 않는 경우
     */
    fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        writeFallback(source, target) { bytes -> decompress(bytes) }

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
