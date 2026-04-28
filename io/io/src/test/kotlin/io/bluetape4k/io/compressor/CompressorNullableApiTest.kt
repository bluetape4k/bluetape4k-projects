package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * [AbstractCompressor.compressOrNull] / [AbstractCompressor.decompressOrNull] 보안 API 테스트.
 *
 * - 정상 입력: 압축/해제 결과 반환
 * - null / empty 입력: `null` 반환
 * - 손상 데이터 역직렬화: `null` 반환 (compress 와 달리 emptyByteArray 대신 null)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressorNullableApiTest {

    companion object: KLogging() {
        @JvmStatic
        fun allCompressors(): Stream<AbstractCompressor> = Stream.of(
            LZ4Compressor(),
            ZstdCompressor(),
            SnappyCompressor(),
            GZipCompressor(),
            DeflateCompressor(),
            BZip2Compressor(),
        )
    }

    // ────────────────────────────────────────────────────────────────────────────
    // compressOrNull: 정상 경로
    // ────────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "compressOrNull 정상 입력: {0}")
    @MethodSource("allCompressors")
    fun `compressOrNull 은 정상 입력에 대해 비어있지 않은 ByteArray 를 반환한다`(compressor: AbstractCompressor) {
        val input = "hello compressOrNull".toByteArray()
        val result = compressor.compressOrNull(input)
        result.shouldNotBeNull()
        require(result.isNotEmpty()) { "compressOrNull result should not be empty" }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // compressOrNull: null / empty 입력
    // ────────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "compressOrNull null 입력 null 반환: {0}")
    @MethodSource("allCompressors")
    fun `compressOrNull 은 null 입력에 null 을 반환한다`(compressor: AbstractCompressor) {
        compressor.compressOrNull(null).shouldBeNull()
    }

    @ParameterizedTest(name = "compressOrNull empty 입력 null 반환: {0}")
    @MethodSource("allCompressors")
    fun `compressOrNull 은 empty 입력에 null 을 반환한다`(compressor: AbstractCompressor) {
        compressor.compressOrNull(ByteArray(0)).shouldBeNull()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // decompressOrNull: 정상 경로
    // ────────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "decompressOrNull 라운드트립: {0}")
    @MethodSource("allCompressors")
    fun `compressOrNull 과 decompressOrNull 은 라운드트립을 지원한다`(compressor: AbstractCompressor) {
        val input = "decompressOrNull round-trip test".toByteArray()
        val compressed = compressor.compressOrNull(input)
        compressed.shouldNotBeNull()

        val restored = compressor.decompressOrNull(compressed)
        restored.shouldNotBeNull()
        restored shouldBeEqualTo input
    }

    // ────────────────────────────────────────────────────────────────────────────
    // decompressOrNull: null / empty 입력
    // ────────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "decompressOrNull null 입력 null 반환: {0}")
    @MethodSource("allCompressors")
    fun `decompressOrNull 은 null 입력에 null 을 반환한다`(compressor: AbstractCompressor) {
        compressor.decompressOrNull(null).shouldBeNull()
    }

    @ParameterizedTest(name = "decompressOrNull empty 입력 null 반환: {0}")
    @MethodSource("allCompressors")
    fun `decompressOrNull 은 empty 입력에 null 을 반환한다`(compressor: AbstractCompressor) {
        compressor.decompressOrNull(ByteArray(0)).shouldBeNull()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // decompressOrNull: 손상 데이터 → null (decompress 는 emptyByteArray)
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `decompressOrNull 은 손상된 LZ4 데이터에 null 을 반환한다`() {
        val compressor = LZ4Compressor()
        val input = "Hello LZ4 corruption test".toByteArray()
        val compressed = compressor.compress(input)

        // 헤더를 손상시켜 역직렬화 실패 유도
        val corrupted = compressed.copyOf()
        corrupted[0] = 0x7F
        corrupted[1] = 0xFF.toByte()
        corrupted[2] = 0xFF.toByte()
        corrupted[3] = 0xFF.toByte()

        // decompressOrNull 은 null 반환
        compressor.decompressOrNull(corrupted).shouldBeNull()
    }

    @Test
    fun `decompressOrNull 은 손상된 GZip 데이터에 null 을 반환한다`() {
        val compressor = GZipCompressor()

        // GZip 매직 바이트(0x1f 0x8b)를 무효화하면 반드시 파싱 실패
        val corrupted = byteArrayOf(0x00, 0x00, 0x01, 0x02, 0x03)

        compressor.decompressOrNull(corrupted).shouldBeNull()
    }

    @Test
    fun `decompressOrNull vs decompress - 손상 데이터에 대한 반환값이 다르다`() {
        val compressor = LZ4Compressor()
        val input = "contrast test".toByteArray()
        val compressed = compressor.compress(input)

        val corrupted = compressed.copyOf()
        corrupted[0] = 0x7F
        corrupted[1] = 0xFF.toByte()
        corrupted[2] = 0xFF.toByte()
        corrupted[3] = 0xFF.toByte()

        // decompress: emptyByteArray 반환 (기존 계약 유지)
        val decompressResult = compressor.decompress(corrupted)
        decompressResult shouldBeEqualTo ByteArray(0)

        // decompressOrNull: null 반환 (손상 여부를 호출자가 구분 가능)
        compressor.decompressOrNull(corrupted).shouldBeNull()
    }
}
