package io.bluetape4k.tink.aead

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.AbstractTinkTest
import io.bluetape4k.tink.aeadKeysetHandle
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkAeadTest: AbstractTinkTest() {

    companion object: KLogging()

    private val aead = TinkAead(aeadKeysetHandle())

    @RepeatedTest(REPEAT_SIZE)
    fun `바이트 배열 encrypt decrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = aead.encrypt(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `문자열 encrypt decrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph()
        val ciphertext = aead.encrypt(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `associatedData가 있는 encrypt decrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val associatedData = "context-id=42".toUtf8Bytes()

        val ciphertext = aead.encrypt(plaintext, associatedData)
        aead.decrypt(ciphertext, associatedData) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `잘못된 associatedData로 decrypt시 예외 발생`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val associatedData = "올바른-컨텍스트".toUtf8Bytes()
        val wrongAssociatedData = "잘못된-컨텍스트".toUtf8Bytes()

        val ciphertext = aead.encrypt(plaintext, associatedData)

        assertFailsWith<GeneralSecurityException> {
            aead.decrypt(ciphertext, wrongAssociatedData)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `빈 associatedData 없이 암호화한 것을 associatedData로 decrypt시 예외 발생`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = aead.encrypt(plaintext) // associatedData = EMPTY_BYTES
        val wrongAd = "context".toUtf8Bytes()

        assertFailsWith<GeneralSecurityException> {
            aead.decrypt(ciphertext, wrongAd)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `동일 평문에 대해 nonce 랜덤화로 다른 암호문 생성`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
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

    @RepeatedTest(REPEAT_SIZE)
    fun `다른 키로 decrypt시 예외 발생`() {
        val aead2 = TinkAead(aeadKeysetHandle())
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = aead.encrypt(plaintext)

        assertFailsWith<GeneralSecurityException> {
            aead2.decrypt(ciphertext)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `변조된 암호문으로 decrypt시 예외 발생`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = aead.encrypt(plaintext)
        val tampered = ciphertext.copyOf()
            .apply {
                this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte()
            }

        assertFailsWith<GeneralSecurityException> {
            aead.decrypt(tampered)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `빈 바이트 배열 encrypt decrypt 라운드트립`() {
        val plaintext = ByteArray(0)
        val ciphertext = aead.encrypt(plaintext)
        aead.decrypt(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AES128_GCM encrypt decrypt 라운드트립`() {
        val aead128 = TinkAeads.AES128_GCM
        val plaintext = faker.lorem().paragraph()

        val encrypted = aead128.encrypt(plaintext)
        aead128.decrypt(encrypted) shouldBeEqualTo plaintext
    }
}
