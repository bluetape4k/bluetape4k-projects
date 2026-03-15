package io.bluetape4k.tink.digest

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
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
