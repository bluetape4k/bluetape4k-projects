package io.bluetape4k.codec

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class StringEncoderSupportTest {

    private val text = "hello-블루테이프"
    private val bytes = text.toByteArray()

    @Test
    fun `Base64 인코딩 디코딩 round-trip`() {
        val encodedStr = bytes.encodeBase64String()
        encodedStr.isNotBlank().shouldBeTrue()

        val decoded = encodedStr.decodeBase64String()
        decoded shouldBeEqualTo text

        val encodedBytes = bytes.encodeBase64ByteArray()
        encodedBytes.isNotEmpty().shouldBeTrue()
        val decodedBytes = encodedBytes.decodeBase64ByteArray()
        String(decodedBytes) shouldBeEqualTo text
    }

    @Test
    fun `Hex 인코딩 디코딩 round-trip`() {
        val hexStr = bytes.encodeHexString()
        hexStr.length shouldBeEqualTo bytes.size * 2

        val decoded = hexStr.decodeHexString()
        decoded shouldBeEqualTo text

        val hexBytes = bytes.encodeHexByteArray()
        hexBytes.isNotEmpty().shouldBeTrue()
        val decodedBytes = hexBytes.decodeHexByteArray()
        String(decodedBytes) shouldBeEqualTo text
    }
}
