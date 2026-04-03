package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class BOMSupportTest: AbstractIOTest() {

    companion object: KLogging()

    // BOM 마커 상수
    private val UTF_32BE_BOM = byteArrayOf(0x00, 0x00, 0xFE.toByte(), 0xFF.toByte())
    private val UTF_32LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00)
    private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF_16BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    private val UTF_16LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())

    private val payload = "Hello, BOM!".toByteArray(Charsets.UTF_8)

    // ──────────────────────────────────────────────────
    // ByteArray.getBOM
    // ──────────────────────────────────────────────────

    @Test
    fun `getBOM - UTF-32 BE BOM 감지`() {
        val bytes = UTF_32BE_BOM + payload
        val (skipSize, charset) = bytes.getBOM()
        skipSize shouldBeEqualTo 4
        charset shouldBeEqualTo Charsets.UTF_32BE
    }

    @Test
    fun `getBOM - UTF-32 LE BOM 감지`() {
        val bytes = UTF_32LE_BOM + payload
        val (skipSize, charset) = bytes.getBOM()
        skipSize shouldBeEqualTo 4
        charset shouldBeEqualTo Charsets.UTF_32LE
    }

    @Test
    fun `getBOM - UTF-8 BOM 감지`() {
        val bytes = UTF_8_BOM + payload
        val (skipSize, charset) = bytes.getBOM()
        skipSize shouldBeEqualTo 3
        charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `getBOM - UTF-16 BE BOM 감지`() {
        val bytes = UTF_16BE_BOM + payload
        val (skipSize, charset) = bytes.getBOM()
        skipSize shouldBeEqualTo 2
        charset shouldBeEqualTo Charsets.UTF_16BE
    }

    @Test
    fun `getBOM - UTF-16 LE BOM 감지`() {
        val bytes = UTF_16LE_BOM + payload
        val (skipSize, charset) = bytes.getBOM()
        skipSize shouldBeEqualTo 2
        charset shouldBeEqualTo Charsets.UTF_16LE
    }

    @Test
    fun `getBOM - BOM 없으면 0과 기본 문자셋 반환`() {
        val (skipSize, charset) = payload.getBOM()
        skipSize shouldBeEqualTo 0
        charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `getBOM - BOM 없을 때 커스텀 기본 문자셋 반환`() {
        val (skipSize, charset) = payload.getBOM(Charsets.ISO_8859_1)
        skipSize shouldBeEqualTo 0
        charset shouldBeEqualTo Charsets.ISO_8859_1
    }

    @Test
    fun `getBOM - 빈 배열은 기본 문자셋 반환`() {
        val (skipSize, charset) = byteArrayOf().getBOM()
        skipSize shouldBeEqualTo 0
        charset shouldBeEqualTo Charsets.UTF_8
    }

    // ──────────────────────────────────────────────────
    // ByteArray.removeBom
    // ──────────────────────────────────────────────────

    @Test
    fun `removeBom - UTF-8 BOM 제거 후 본문 데이터 반환`() {
        val bytes = UTF_8_BOM + payload
        val (data, charset) = bytes.removeBom()
        data shouldBeEqualTo payload
        charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `removeBom - UTF-16 BE BOM 제거`() {
        val bytes = UTF_16BE_BOM + payload
        val (data, charset) = bytes.removeBom()
        data shouldBeEqualTo payload
        charset shouldBeEqualTo Charsets.UTF_16BE
    }

    @Test
    fun `removeBom - UTF-32 BE BOM 제거`() {
        val bytes = UTF_32BE_BOM + payload
        val (data, charset) = bytes.removeBom()
        data shouldBeEqualTo payload
        charset shouldBeEqualTo Charsets.UTF_32BE
    }

    @Test
    fun `removeBom - BOM 없으면 원본 배열 그대로 반환`() {
        val (data, charset) = payload.removeBom()
        data shouldBeEqualTo payload
        charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `removeBom - 실제 UTF-8 BOM 포함 문자열 복원`() {
        val text = "안녕하세요, 세계!"
        val bytes = UTF_8_BOM + text.toByteArray(Charsets.UTF_8)
        val (data, charset) = bytes.removeBom()
        String(data, charset) shouldBeEqualTo text
    }

    // ──────────────────────────────────────────────────
    // InputStream.withoutBom
    // ──────────────────────────────────────────────────

    @Test
    fun `withoutBom - UTF-8 BOM 포함 스트림에서 BOM 제거`() {
        val bytes = UTF_8_BOM + payload
        val (stream, charset) = ByteArrayInputStream(bytes).withoutBom()
        charset shouldBeEqualTo Charsets.UTF_8
        stream.readBytes() shouldBeEqualTo payload
    }

    @Test
    fun `withoutBom - UTF-16 BE BOM 포함 스트림에서 BOM 제거`() {
        val bytes = UTF_16BE_BOM + payload
        val (stream, charset) = ByteArrayInputStream(bytes).withoutBom()
        charset shouldBeEqualTo Charsets.UTF_16BE
        stream.readBytes() shouldBeEqualTo payload
    }

    @Test
    fun `withoutBom - UTF-32 LE BOM 포함 스트림에서 BOM 제거`() {
        val bytes = UTF_32LE_BOM + payload
        val (stream, charset) = ByteArrayInputStream(bytes).withoutBom()
        charset shouldBeEqualTo Charsets.UTF_32LE
        stream.readBytes() shouldBeEqualTo payload
    }

    @Test
    fun `withoutBom - BOM 없는 스트림은 전체 데이터 그대로 읽힘`() {
        val (stream, charset) = ByteArrayInputStream(payload).withoutBom()
        charset shouldBeEqualTo Charsets.UTF_8
        stream.readBytes() shouldBeEqualTo payload
    }

    @Test
    fun `withoutBom - 커스텀 기본 문자셋 적용`() {
        val (stream, charset) = ByteArrayInputStream(payload).withoutBom(Charsets.ISO_8859_1)
        charset shouldBeEqualTo Charsets.ISO_8859_1
        stream.readBytes().isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `withoutBom - 실제 UTF-8 BOM 포함 문자열 복원`() {
        val text = "Kotlin BOM 테스트"
        val bytes = UTF_8_BOM + text.toByteArray(Charsets.UTF_8)
        val (stream, charset) = ByteArrayInputStream(bytes).withoutBom()
        stream.bufferedReader(charset).readText() shouldBeEqualTo text
    }
}
