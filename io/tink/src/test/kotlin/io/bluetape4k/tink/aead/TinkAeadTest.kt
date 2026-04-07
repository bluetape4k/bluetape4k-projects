package io.bluetape4k.tink.aead

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aeadKeysetHandle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkAeadTest {
    companion object: KLogging()

    private val aead = TinkAead(aeadKeysetHandle())

    @Test
    fun `바이트 배열 encrypt decrypt 라운드트립`() {
        val plaintext = "Hello, World!".toByteArray()
        val ciphertext = aead.encrypt(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `문자열 encrypt decrypt 라운드트립`() {
        val plaintext = "안녕하세요, Tink!"
        val ciphertext = aead.encrypt(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `associatedData가 있는 encrypt decrypt 라운드트립`() {
        val plaintext = "비밀 데이터".toByteArray()
        val associatedData = "context-id=42".toByteArray()

        val ciphertext = aead.encrypt(plaintext, associatedData)
        aead.decrypt(ciphertext, associatedData) shouldBeEqualTo plaintext
    }

    @Test
    fun `잘못된 associatedData로 decrypt시 예외 발생`() {
        val plaintext = "비밀 데이터".toByteArray()
        val associatedData = "올바른-컨텍스트".toByteArray()
        val wrongAssociatedData = "잘못된-컨텍스트".toByteArray()

        val ciphertext = aead.encrypt(plaintext, associatedData)

        assertThrows<GeneralSecurityException> {
            aead.decrypt(ciphertext, wrongAssociatedData)
        }
    }

    @Test
    fun `빈 associatedData 없이 암호화한 것을 associatedData로 decrypt시 예외 발생`() {
        val plaintext = "비밀".toByteArray()
        val ciphertext = aead.encrypt(plaintext) // associatedData = EMPTY_BYTES
        val wrongAd = "context".toByteArray()

        assertThrows<GeneralSecurityException> {
            aead.decrypt(ciphertext, wrongAd)
        }
    }

    @Test
    fun `동일 평문에 대해 nonce 랜덤화로 다른 암호문 생성`() {
        val plaintext = "같은 메시지".toByteArray()
        val ct1 = aead.encrypt(plaintext)
        val ct2 = aead.encrypt(plaintext)

        // AEAD는 nonce를 랜덤하게 생성하므로 동일 평문이라도 암호문이 달라야 함
        ct1 shouldNotBeEqualTo ct2
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 테스트", "special chars !@#\$%^&*()"])
    fun `다양한 문자열 encrypt decrypt 라운드트립`(plaintext: String) {
        val encrypted = aead.encrypt(plaintext)
        val decrypted = aead.decrypt(encrypted)
        decrypted shouldBeEqualTo plaintext
    }

    @Test
    fun `다른 키로 decrypt시 예외 발생`() {
        val aead2 = TinkAead(aeadKeysetHandle())
        val plaintext = "비밀 데이터".toByteArray()
        val ciphertext = aead.encrypt(plaintext)

        assertThrows<GeneralSecurityException> {
            aead2.decrypt(ciphertext)
        }
    }

    @Test
    fun `변조된 암호문으로 decrypt시 예외 발생`() {
        val plaintext = "변조 테스트".toByteArray()
        val ciphertext = aead.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertThrows<GeneralSecurityException> {
            aead.decrypt(tampered)
        }
    }

    @Test
    fun `빈 바이트 배열 encrypt decrypt 라운드트립`() {
        val plaintext = ByteArray(0)
        val ciphertext = aead.encrypt(plaintext)
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `AES128_GCM encrypt decrypt 라운드트립`() {
        val aead128 = TinkAeads.AES128_GCM
        val plaintext = "AES128 테스트"
        val encrypted = aead128.encrypt(plaintext)
        aead128.decrypt(encrypted) shouldBeEqualTo plaintext
    }
}
