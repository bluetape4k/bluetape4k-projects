package io.bluetape4k.okio

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import okio.ByteString
import org.junit.jupiter.api.Test

/**
 * [ByteStringSupport.kt]의 `byteStringOf` 함수들을 검증합니다.
 */
class ByteStringSupportTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `byteStringOf vararg bytes는 바이트 배열로부터 ByteString을 생성한다`() {
        val bs = byteStringOf(0x48.toByte(), 0x69.toByte())
        bs.size shouldBeEqualTo 2
        bs.utf8() shouldBeEqualTo "Hi"
    }

    @Test
    fun `byteStringOf ByteArray는 바이트 배열로부터 ByteString을 생성한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val bs = byteStringOf(bytes)
        bs.size shouldBeEqualTo 3
        bs shouldBeEqualTo ByteString.of(*bytes)
    }

    @Test
    fun `byteStringOf text는 문자열로부터 ByteString을 생성한다`() {
        val bs = byteStringOf("hello")
        bs.size shouldBeEqualTo 5
        bs.utf8() shouldBeEqualTo "hello"
    }

    @Test
    fun `byteStringOf text 한국어는 올바르게 인코딩된다`() {
        val text = "안녕"
        val bs = byteStringOf(text)
        bs.utf8() shouldBeEqualTo text
    }

    @Test
    fun `byteStringOf text with charset은 지정된 charset으로 인코딩한다`() {
        val text = "Hello"
        val bs = byteStringOf(text, Charsets.ISO_8859_1)
        bs.size shouldBeEqualTo 5
        bs.string(Charsets.ISO_8859_1) shouldBeEqualTo text
    }

    @Test
    fun `byteStringOf 빈 바이트 배열은 빈 ByteString을 생성한다`() {
        val bs = byteStringOf(ByteArray(0))
        bs.size shouldBeEqualTo 0
        bs shouldBeEqualTo ByteString.EMPTY
    }
}
