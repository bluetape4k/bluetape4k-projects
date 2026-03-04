package io.bluetape4k.tink.encrypt

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
        val encryptors = listOf(
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
}
