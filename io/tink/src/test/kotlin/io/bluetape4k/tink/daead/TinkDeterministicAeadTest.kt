package io.bluetape4k.tink.daead

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.daeadKeysetHandle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.security.GeneralSecurityException

class TinkDeterministicAeadTest {
    companion object : KLogging()

    private val daead = TinkDeterministicAead(daeadKeysetHandle())

    @Test
    fun `바이트 배열 encryptDeterministically decrypt 라운드트립`() {
        val plaintext = "결정적 암호화 테스트".toByteArray()
        val ciphertext = daead.encryptDeterministically(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        daead.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `문자열 encryptDeterministically decrypt 라운드트립`() {
        val plaintext = "결정적 문자열 암호화"
        val ciphertext = daead.encryptDeterministically(plaintext)

        ciphertext shouldNotBeEqualTo plaintext
        daead.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `동일한 평문은 항상 동일한 암호문을 생성한다 (결정적 특성)`() {
        val plaintext = "검색 가능한 필드 값".toByteArray()

        val ct1 = daead.encryptDeterministically(plaintext)
        val ct2 = daead.encryptDeterministically(plaintext)

        // Deterministic AEAD의 핵심 특성: 동일 입력 -> 동일 출력
        ct1 shouldBeEqualTo ct2
    }

    @Test
    fun `associatedData가 있는 라운드트립`() {
        val plaintext = "민감한 데이터".toByteArray()
        val associatedData = "table=users,column=email".toByteArray()

        val ciphertext = daead.encryptDeterministically(plaintext, associatedData)
        daead.decryptDeterministically(ciphertext, associatedData) shouldBeEqualTo plaintext
    }

    @Test
    fun `잘못된 associatedData로 decrypt시 예외 발생`() {
        val plaintext = "데이터".toByteArray()
        val correctAd = "올바른-AD".toByteArray()
        val wrongAd = "잘못된-AD".toByteArray()

        val ciphertext = daead.encryptDeterministically(plaintext, correctAd)

        assertThrows<GeneralSecurityException> {
            daead.decryptDeterministically(ciphertext, wrongAd)
        }
    }

    @Test
    fun `TinkDaeads 싱글턴 AES256_SIV 라운드트립`() {
        val plaintext = "싱글턴 인스턴스 테스트"
        val ciphertext = TinkDaeads.AES256_SIV.encryptDeterministically(plaintext)
        TinkDaeads.AES256_SIV.decryptDeterministically(ciphertext) shouldBeEqualTo plaintext
    }

    @Test
    fun `TinkDaeads 싱글턴 동일 평문 결정적 특성 검증`() {
        val plaintext = "검색 인덱스 값"
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

    @Test
    fun `다른 키로 decryptDeterministically시 예외 발생`() {
        val daead2 = TinkDeterministicAead(daeadKeysetHandle())
        val plaintext = "비밀 필드".toByteArray()
        val ciphertext = daead.encryptDeterministically(plaintext)

        assertThrows<GeneralSecurityException> {
            daead2.decryptDeterministically(ciphertext)
        }
    }

    @Test
    fun `변조된 암호문으로 decryptDeterministically시 예외 발생`() {
        val plaintext = "변조 테스트".toByteArray()
        val ciphertext = daead.encryptDeterministically(plaintext)
        val tampered = ciphertext.copyOf().apply { this[ciphertext.size / 2] = (this[ciphertext.size / 2].toInt() xor 0xFF).toByte() }

        assertThrows<GeneralSecurityException> {
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
