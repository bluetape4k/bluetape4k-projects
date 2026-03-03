package io.bluetape4k.tink.mac

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.macKeysetHandle
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TinkMacTest {

    companion object : KLogging()

    private val mac = TinkMac(macKeysetHandle())

    @Test
    fun `바이트 배열 computeMac 후 verifyMac 성공`() {
        val data = "Hello, MAC!".toByteArray()
        val tag = mac.computeMac(data)

        tag shouldNotBeEqualTo data
        mac.verifyMac(tag, data).shouldBeTrue()
    }

    @Test
    fun `문자열 computeMac 후 verifyMac 성공`() {
        val data = "안녕하세요, MAC 검증!"
        val tag = mac.computeMac(data)

        mac.verifyMac(tag, data).shouldBeTrue()
    }

    @Test
    fun `변조된 데이터로 verifyMac 실패`() {
        val data = "원본 데이터".toByteArray()
        val tag = mac.computeMac(data)

        val tamperedData = "변조된 데이터".toByteArray()
        mac.verifyMac(tag, tamperedData).shouldBeFalse()
    }

    @Test
    fun `변조된 태그로 verifyMac 실패`() {
        val data = "데이터".toByteArray()
        val tag = mac.computeMac(data)

        val tamperedTag = tag.copyOf()
        tamperedTag[0] = (tamperedTag[0].toInt() xor 0xFF).toByte()

        mac.verifyMac(tamperedTag, data).shouldBeFalse()
    }

    @Test
    fun `TinkMacs HMAC_SHA256 싱글턴 computeMac verifyMac`() {
        val data = "HMAC-SHA256 테스트"
        val tag = TinkMacs.HMAC_SHA256.computeMac(data)
        TinkMacs.HMAC_SHA256.verifyMac(tag, data).shouldBeTrue()
    }

    @Test
    fun `TinkMacs HMAC_SHA512 싱글턴 computeMac verifyMac`() {
        val data = "HMAC-SHA512 테스트"
        val tag = TinkMacs.HMAC_SHA512.computeMac(data)
        TinkMacs.HMAC_SHA512.verifyMac(tag, data).shouldBeTrue()
    }

    @Test
    fun `서로 다른 MAC 인스턴스의 태그는 교차 검증 불가`() {
        val mac1 = TinkMac(macKeysetHandle())
        val mac2 = TinkMac(macKeysetHandle())
        val data = "교차 검증 테스트".toByteArray()

        val tag = mac1.computeMac(data)
        // 다른 키를 사용한 mac2로는 검증 실패
        mac2.verifyMac(tag, data).shouldBeFalse()
    }

    @Test
    fun `확장 함수 computeTinkMac verifyTinkMac 바이트 배열`() {
        val data = "확장 함수 테스트".toByteArray()
        val tag = data.computeTinkMac(mac)
        data.verifyTinkMac(tag, mac).shouldBeTrue()
    }

    @Test
    fun `확장 함수 computeTinkMac verifyTinkMac 문자열`() {
        val data = "문자열 확장 함수"
        val tag = data.computeTinkMac(mac)
        data.verifyTinkMac(tag, mac).shouldBeTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 데이터", "special !@#\$%"])
    fun `다양한 문자열 computeMac verifyMac`(data: String) {
        val tag = mac.computeMac(data)
        mac.verifyMac(tag, data).shouldBeTrue()
    }
}
