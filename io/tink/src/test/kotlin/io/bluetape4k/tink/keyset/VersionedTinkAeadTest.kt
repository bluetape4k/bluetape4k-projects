package io.bluetape4k.tink.keyset

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class VersionedTinkAeadTest {

    companion object : KLogging()

    private fun newVersionedAead() = VersionedTinkAead(InMemoryVersionedKeysetStore())

    @Test
    fun `바이트 배열 encrypt decrypt 라운드트립`() {
        val va = newVersionedAead()
        val plaintext = "Hello, VersionedTinkAead!".toByteArray()
        val ciphertext = va.encrypt(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        va.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `문자열 encrypt decrypt 라운드트립`() {
        val va = newVersionedAead()
        val plaintext = "버전 키셋 암호화 테스트"
        val encrypted = va.encrypt(plaintext)

        encrypted shouldNotBeEqualTo plaintext
        va.decrypt(encrypted) shouldBeEqualTo plaintext
    }

    @Test
    fun `associatedData를 포함한 encrypt decrypt 라운드트립`() {
        val va = newVersionedAead()
        val plaintext = "컨텍스트 포함 암호화".toByteArray()
        val ad = "user-id=42".toByteArray()

        val ciphertext = va.encrypt(plaintext, ad)
        va.decrypt(ciphertext, ad) shouldBeEqualTo plaintext
    }

    @Test
    fun `암호문에 버전 정보가 포함되어 있음`() {
        val va = newVersionedAead()
        val plaintext = "버전 포함 테스트".toByteArray()
        val ciphertext = va.encrypt(plaintext)

        // 버전 정보(Long = 8 bytes) + ciphertext
        (ciphertext.size > Long.SIZE_BYTES).shouldBeTrue()
    }

    @Test
    fun `currentVersion 은 1부터 시작`() {
        val va = newVersionedAead()
        va.currentVersion() shouldBeEqualTo 1L
    }

    @Test
    fun `rotate 후 currentVersion 증가`() {
        val va = newVersionedAead()
        va.currentVersion() // ensure store initialised
        val newHandle = va.rotate()

        (newHandle.version > 1L).shouldBeTrue()
        (va.currentVersion() >= 2L).shouldBeTrue()
    }

    @Test
    fun `rotate 후 새 키로 암호화, 이전 키로 복호화 불가`() {
        val store = InMemoryVersionedKeysetStore()
        val va = VersionedTinkAead(store)

        val plaintext = "이전 버전 테스트".toByteArray()
        val oldCiphertext = va.encrypt(plaintext)

        va.rotate()

        // 새 버전으로 재암호화
        val newCiphertext = va.encrypt(plaintext)

        // 새 버전으로 암호화된 것은 현재 keyset으로 복호화 가능
        va.decrypt(newCiphertext) shouldBeEqualTo plaintext
        // 이전 버전으로 암호화된 것도 store가 보유하므로 복호화 가능
        va.decrypt(oldCiphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `알 수 없는 버전으로 복호화시 예외 발생`() {
        val va = newVersionedAead()
        // 버전 99를 가리키는 가짜 payload 생성
        val fakePayload = packVersionedCiphertext(99L, "garbage".toByteArray())

        assertThrows<IllegalArgumentException> {
            va.decrypt(fakePayload)
        }
    }

    @Test
    fun `rotateIfDue - 기간 미경과시 회전 없음`() {
        val va = newVersionedAead()
        val before = va.currentVersion()
        va.rotateIfDue(Duration.ofDays(30))
        va.currentVersion() shouldBeEqualTo before
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 테스트", "special !@#\$%^&*()"])
    fun `다양한 문자열 encrypt decrypt 라운드트립`(plaintext: String) {
        val va = newVersionedAead()
        val encrypted = va.encrypt(plaintext)
        va.decrypt(encrypted) shouldBeEqualTo plaintext
    }
}
