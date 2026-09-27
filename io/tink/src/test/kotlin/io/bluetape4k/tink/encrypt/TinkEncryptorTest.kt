package io.bluetape4k.tink.encrypt

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.AbstractTinkTest
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.daeadKeysetHandle
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkEncryptorTest: AbstractTinkTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD 바이트 배열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD 문자열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = faker.lorem().paragraph()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD 동일 평문에 대해 다른 암호문 생성 (비결정적)`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = faker.lorem().paragraph()

        val ct1 = encryptor.encrypt(plaintext)
        val ct2 = encryptor.encrypt(plaintext)
        ct1 shouldNotBeEqualTo ct2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD 바이트 배열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD 문자열 encrypt decrypt 라운드트립`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = faker.lorem().paragraph()

        val ciphertext = encryptor.encrypt(plaintext)
        ciphertext shouldNotBeEqualTo plaintext
        encryptor.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD 동일 평문에 대해 동일 암호문 생성 (결정적)`() {
        val encryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV
        val plaintext = faker.lorem().paragraph()

        val ct1 = encryptor.encrypt(plaintext)
        val ct2 = encryptor.encrypt(plaintext)
        ct1 shouldBeEqualTo ct2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `모든 AEAD encryptor 변형 라운드트립`() {
        val encryptors =
            listOf(
                TinkEncryptors.AES256_GCM,
                TinkEncryptors.AES128_GCM,
                TinkEncryptors.CHACHA20_POLY1305,
                TinkEncryptors.XCHACHA20_POLY1305,
            )
        val plaintext = faker.lorem().paragraph()

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

    @RepeatedTest(REPEAT_SIZE)
    fun `extension 함수 encrypt decrypt 동작 확인`() {
        val encryptor = TinkEncryptors.AES256_GCM
        val plaintext = faker.lorem().paragraph()

        val encrypted = plaintext.tinkEncrypt(encryptor)
        encrypted.tinkDecrypt(encryptor) shouldBeEqualTo plaintext

        val byteData = plaintext.toUtf8Bytes()
        val byteEncrypted = byteData.tinkEncrypt(encryptor)
        byteEncrypted.tinkDecrypt(encryptor) shouldBeEqualTo byteData
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD 다른 키로 decrypt시 예외 발생`() {
        val encryptor1 = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))
        val encryptor2 = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))

        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = encryptor1.encrypt(plaintext)

        assertFailsWith<GeneralSecurityException> {
            encryptor2.decrypt(ciphertext)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AEAD 변조된 암호문으로 decrypt시 예외 발생`() {
        val encryptor = TinkAeadEncryptor(TinkAead(aeadKeysetHandle()))
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = encryptor.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply {
                this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte()
            }

        assertFailsWith<GeneralSecurityException> {
            encryptor.decrypt(tampered)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD 다른 키로 decrypt시 예외 발생`() {
        val encryptor1 = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))
        val encryptor2 = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))

        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = encryptor1.encrypt(plaintext)

        assertFailsWith<GeneralSecurityException> {
            encryptor2.decrypt(ciphertext)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `DAEAD 변조된 암호문으로 decrypt시 예외 발생`() {
        val encryptor = TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle()))
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = encryptor.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertFailsWith<GeneralSecurityException> {
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
