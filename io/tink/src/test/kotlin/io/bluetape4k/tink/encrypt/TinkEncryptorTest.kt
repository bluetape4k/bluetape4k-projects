package io.bluetape4k.tink.encrypt

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.daeadKeysetHandle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkEncryptorTest {
    companion object: KLogging()

    @Test
    fun `AEAD 바이트 배열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = "Hello, World!".toByteArray()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `AEAD 문자열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = "안녕하세요, Tink!"

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `AEAD 동일 평문에 대해 다른 암호문 생성 (비결정적)`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = "같은 메시지"

        val ct1 = encryptor.encrypt(plaintext)
        val ct2 = encryptor.encrypt(plaintext)
        ct1 shouldNotBeEqualTo ct2
    }

    @Test
    fun `DAEAD 바이트 배열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = "Hello, World!".toByteArray()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `DAEAD 문자열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = "검색 가능한 필드 값"

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `DAEAD 동일 평문에 대해 동일 암호문 생성 (결정적)`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = "검색 가능한 필드 값"

        val ct1 = encryptor.encrypt(plaintext)
        val ct2 = encryptor.encrypt(plaintext)
        ct1 shouldBeEqualTo ct2
    }

    @Test
    fun `모든 AEAD encryptor 변형 라운드트립`() {
        val encryptors =
            listOf(
                TinkEncryptors.AES256_GCM,
                TinkEncryptors.AES128_GCM,
                TinkEncryptors.CHACHA20_POLY1305,
                TinkEncryptors.XCHACHA20_POLY1305,
            )
        val plaintext = "테스트 데이터"

        encryptors.forEach { encryptor ->
            val ciphertext = encryptor.encrypt(plaintext)
            encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 테스트", "special chars !@#\$%^&*()"])
    fun `다양한 문자열 AEAD encrypt decrypt 라운드트립`(plaintext: String) {
        val encryptor = TinkEncryptors.AES256_GCM
        val encrypted = encryptor.encrypt(plaintext)
        encryptor.decrypt(encrypted) shouldBeEqualTo plaintext
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "한글 테스트", "special chars !@#\$%^&*()"])
    fun `다양한 문자열 DAEAD encrypt decrypt 라운드트립`(plaintext: String) {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val encrypted = encryptor.encrypt(plaintext)
        encryptor.decrypt(encrypted) shouldBeEqualTo plaintext
    }

    @Test
    fun `extension 함수 encrypt decrypt 동작 확인`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = "Hello, World!"

        val encrypted = plaintext.tinkEncrypt(encryptor)
        encrypted.tinkDecrypt(encryptor) shouldBeEqualTo plaintext

        val byteData = plaintext.toByteArray()
        val byteEncrypted = byteData.tinkEncrypt(encryptor)
        byteEncrypted.tinkDecrypt(encryptor) shouldBeEqualTo byteData
    }

    @Test
    fun `AEAD 다른 키로 decrypt시 예외 발생`() {
        val encryptor1 = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))
        val encryptor2 = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))
        val plaintext = "비밀 메시지".toByteArray()
        val ciphertext = encryptor1.encrypt(plaintext)

        assertThrows<GeneralSecurityException> {
            encryptor2.decrypt(ciphertext)
        }
    }

    @Test
    fun `AEAD 변조된 암호문으로 decrypt시 예외 발생`() {
        val encryptor = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))
        val plaintext = "변조 테스트".toByteArray()
        val ciphertext = encryptor.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertThrows<GeneralSecurityException> {
            encryptor.decrypt(tampered)
        }
    }

    @Test
    fun `DAEAD 다른 키로 decrypt시 예외 발생`() {
        val encryptor1 = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))
        val encryptor2 = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))
        val plaintext = "검색 가능한 필드".toByteArray()
        val ciphertext = encryptor1.encrypt(plaintext)

        assertThrows<GeneralSecurityException> {
            encryptor2.decrypt(ciphertext)
        }
    }

    @Test
    fun `DAEAD 변조된 암호문으로 decrypt시 예외 발생`() {
        val encryptor = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))
        val plaintext = "변조 DAEAD 테스트".toByteArray()
        val ciphertext = encryptor.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertThrows<GeneralSecurityException> {
            encryptor.decrypt(tampered)
        }
    }

    @Test
    fun `빈 바이트 배열 AEAD encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = ByteArray(0)
        encryptor.decrypt(encryptor.encrypt(plaintext)) shouldBeEqualTo plaintext
    }

    @Test
    fun `빈 바이트 배열 DAEAD encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = ByteArray(0)
        encryptor.decrypt(encryptor.encrypt(plaintext)) shouldBeEqualTo plaintext
    }
}
