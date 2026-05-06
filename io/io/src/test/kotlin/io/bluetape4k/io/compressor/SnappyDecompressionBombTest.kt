package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.xerial.snappy.Snappy

/**
 * [SnappyCompressor] decompression bomb 방어 테스트.
 *
 * - 256MB 초과 uncompressed_length를 선언하는 페이로드에 대해 [IllegalArgumentException] 전파 검증
 * - 정상 데이터는 정상 처리됨 검증
 */
class SnappyDecompressionBombTest {

    companion object: KLogging()

    private val compressor = SnappyCompressor()

    @Test
    fun `정상 데이터는 압축 해제 성공`() {
        val original = "Hello, Snappy! 압축 테스트".toByteArray()
        val compressed = compressor.compress(original)
        val restored = compressor.decompress(compressed)
        restored.shouldNotBeEmpty()
    }

    @Test
    fun `decompress - 256MB 초과 선언 페이로드는 IllegalArgumentException 전파`() {
        // Snappy 헤더: varint로 인코딩된 uncompressed_length (256MB + 1)
        // Wave 1 패치의 require() 검사가 AbstractCompressor.decompress()를 통해 호출자에게 전파됨
        val fakeCompressed = encodeSnappyHeader(256L * 1024 * 1024 + 1)

        assertThrows<IllegalArgumentException> {
            compressor.decompress(fakeCompressed)
        }
    }

    @Test
    fun `decompress - 정확히 256MB 이하는 require 통과 (실제 압축 데이터 사용)`() {
        // 소규모 정상 데이터로 정상 경로 검증 (1MB 미만)
        val data = ByteArray(1024) { it.toByte() }
        val compressed = Snappy.compress(data)

        val uncompressedSize = Snappy.uncompressedLength(compressed)
        assert(uncompressedSize <= 256 * 1024 * 1024) { "테스트 데이터 크기가 한도 초과" }

        val result = compressor.decompress(compressed)
        result.shouldNotBeEmpty()
    }

    /**
     * Snappy 스트림 헤더를 흉내 낸 바이트 배열 생성.
     * uncompressed_length varint만 포함 — [Snappy.uncompressedLength] 호출로 읽힘.
     */
    private fun encodeSnappyHeader(uncompressedSize: Long): ByteArray {
        val buf = mutableListOf<Byte>()
        var v = uncompressedSize
        while (v > 127) {
            buf.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        buf.add((v and 0x7F).toByte())
        buf.addAll(listOf(0, 0, 0, 0, 0).map { it.toByte() })
        return buf.toByteArray()
    }
}
