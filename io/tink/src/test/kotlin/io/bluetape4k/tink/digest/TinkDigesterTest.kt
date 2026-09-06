package io.bluetape4k.tink.digest

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TinkDigesterTest {

    companion object: KLogging()

    private val digester = TinkDigesters.SHA256

    @Test
    fun `바이트 배열 digest 라운드트립`() {
        val data = "Hello, World!".toByteArray()
        val hash = digester.digest(data)

        hash shouldNotBeEqualTo data
        digester.matches(data, hash).shouldBeTrue()
    }

    @Test
    fun `문자열 digest 라운드트립`() {
        val data = "안녕하세요, Tink!"
        val hash = digester.digest(data)

        hash shouldNotBeEqualTo data
        digester.matches(data, hash).shouldBeTrue()
    }

    @Test
    fun `동일한 입력은 항상 동일한 해시를 생성`() {
        val data = "같은 메시지".toByteArray()
        val hash1 = digester.digest(data)
        val hash2 = digester.digest(data)

        hash1 shouldBeEqualTo hash2
    }

    @Test
    fun `다른 입력은 다른 해시를 생성`() {
        val hash1 = digester.digest("메시지1")
        val hash2 = digester.digest("메시지2")

        hash1 shouldNotBeEqualTo hash2
    }

    @Test
    fun `잘못된 해시로 matches 실패`() {
        val data = "Hello".toByteArray()
        val hash = digester.digest(data)
        val wrongHash = hash.copyOf().apply { this[0] = (this[0].toInt() xor 0xFF).toByte() }

        digester.matches(data, wrongHash).shouldBeFalse()
    }

    @Test
    fun `문자열 잘못된 해시로 matches 실패`() {
        val data = "Hello, World!"
        val wrong = "World, Hello!"
        val wrongHash = digester.digest(wrong)

        digester.matches(data, wrongHash).shouldBeFalse()
    }

    @Test
    fun `문자열 malformed Base64 expected hash는 false를 반환`() {
        digester.matches("Hello, World!", "not-base64-!").shouldBeFalse()
    }

    @Test
    fun `문자열을 UTF-8 lowercase hex digest로 변환한다`() {
        val hash = digester.digestHex("abc")

        hash shouldBeEqualTo "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        hash.length shouldBeEqualTo 64
    }

    @Test
    fun `선행 0 바이트를 두 자리 hex로 보존한다`() {
        digester.digestHex("286") shouldBeEqualTo
            "00328ce57bbc14b33bd6695bc8eb32cdf2fb5f3a7d89ec14a42825e15d39df60"
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "한글 테스트", "special chars !@#\$%^&*()"])
    fun `hex digest verifier 라운드트립`(data: String) {
        val hash = digester.digestHex(data)

        digester.matchesHex(data, hash).shouldBeTrue()
        digester.matchesHex("다른 입력", hash).shouldBeFalse()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "not-hex",
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015a",
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
        ],
    )
    fun `잘못된 hex expected hash는 false를 반환한다`(expected: String) {
        digester.matchesHex("abc", expected).shouldBeFalse()
    }

    @Test
    fun `기존 문자열 digest는 Base64 계약을 유지한다`() {
        digester.digest("abc") shouldBeEqualTo "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0="
    }

    @Test
    fun `빈 입력 digest`() {
        val hash = digester.digest(ByteArray(0))
        hash shouldNotBeEqualTo ByteArray(0)

        val strHash = digester.digest("")
        strHash shouldNotBeEqualTo ""
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 테스트", "special chars !@#\$%^&*()"])
    fun `다양한 문자열 digest matches 라운드트립`(data: String) {
        val hash = digester.digest(data)
        digester.matches(data, hash).shouldBeTrue()
    }

    @Test
    fun `모든 알고리즘 digest 동작 확인`() {
        val data = "테스트 데이터"
        val digesters = listOf(
            TinkDigesters.MD5,
            TinkDigesters.SHA1,
            TinkDigesters.SHA256,
            TinkDigesters.SHA384,
            TinkDigesters.SHA512,
        )

        digesters.forEach { d ->
            val hash = d.digest(data)
            d.matches(data, hash).shouldBeTrue()
        }
    }

    @Test
    fun `extension 함수 digest 동작 확인`() {
        val data = "Hello, World!"
        val hash = data.tinkDigest(digester)
        data.matchesTinkDigest(hash, digester).shouldBeTrue()

        val byteData = data.toByteArray()
        val byteHash = byteData.tinkDigest(digester)
        byteData.matchesTinkDigest(byteHash, digester).shouldBeTrue()
    }
}
