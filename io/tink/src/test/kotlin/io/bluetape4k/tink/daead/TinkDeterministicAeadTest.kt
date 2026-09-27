package io.bluetape4k.tink.daead

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.AbstractTinkTest
import io.bluetape4k.tink.daeadKeysetHandle
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkDeterministicAeadTest: AbstractTinkTest() {

    companion object: KLogging()

    private val daead = TinkDeterministicAead(daeadKeysetHandle())

    @RepeatedTest(REPEAT_SIZE)
    fun `바이트 배열 encryptDeterministically decrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = daead.encryptDeterministically(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        daead.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `문자열 encryptDeterministically decrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph()
        val ciphertext = daead.encryptDeterministically(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        daead.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `동일한 평문은 항상 동일한 암호문을 생성한다 (결정적 특성)`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()

        val ct1 = daead.encryptDeterministically(plaintext)
        val ct2 = daead.encryptDeterministically(plaintext)

        // Deterministic AEAD의 핵심 특성: 동일 입력 -> 동일 출력
        ct1 shouldBeEqualTo ct2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `associatedData가 있는 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val associatedData = "table=users,column=email".toUtf8Bytes()

        val ciphertext = daead.encryptDeterministically(plaintext, associatedData)
        daead.decryptDeterministically(ciphertext, associatedData) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `잘못된 associatedData로 decrypt시 예외 발생`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val correctAd = "올바른-AD".toUtf8Bytes()
        val wrongAd = "잘못된-AD".toUtf8Bytes()

        val ciphertext = daead.encryptDeterministically(plaintext, correctAd)

        assertFailsWith<GeneralSecurityException> {
            daead.decryptDeterministically(ciphertext, wrongAd)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `TinkDaeads 싱글턴 AES256_SIV 라운드트립`() {
        val plaintext = faker.lorem().paragraph()
        val ciphertext = TinkDaeads.AES256_SIV.encryptDeterministically(plaintext)
        TinkDaeads.AES256_SIV.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `TinkDaeads 싱글턴 동일 평문 결정적 특성 검증`() {
        val plaintext = faker.lorem().paragraph()
        val ct1 = TinkDaeads.AES256_SIV.encryptDeterministically(plaintext)
        val ct2 = TinkDaeads.AES256_SIV.encryptDeterministically(plaintext)
        ct1 shouldBeEqualTo ct2
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a", "이메일@예시.com", "주민등록번호-123456"])
    fun `다양한 문자열 결정적 encrypt decrypt 라운드트립`(plaintext: String) {
        val ct = daead.encryptDeterministically(plaintext)
        daead.decryptDeterministically(ct) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `다른 키로 decryptDeterministically시 예외 발생`() {
        val daead2 = TinkDeterministicAead(daeadKeysetHandle())
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = daead.encryptDeterministically(plaintext)

        assertFailsWith<GeneralSecurityException> {
            daead2.decryptDeterministically(ciphertext)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `변조된 암호문으로 decryptDeterministically시 예외 발생`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ciphertext = daead.encryptDeterministically(plaintext)
        val tampered = ciphertext.copyOf()
            .apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertFailsWith<GeneralSecurityException> {
            daead.decryptDeterministically(tampered)
        }
    }

    @Test
    fun `빈 바이트 배열 결정적 encrypt decrypt 라운드트립`() {
        val plaintext = ByteArray(0)
        val ciphertext = daead.encryptDeterministically(plaintext)
        daead.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }
}
