package io.bluetape4k.tink.aead

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class TinkAeadExtensionsTest {

    companion object: KLogging()

    private val aead = TinkAeads.AES256_GCM

    @Test
    fun `ByteArray tinkEncrypt 후 tinkDecrypt 라운드트립`() {
        val plaintext = "확장 함수 테스트".toByteArray()
        val encrypted = plaintext.tinkEncrypt(aead)

        encrypted shouldNotBeEqualTo plaintext
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @Test
    fun `String tinkEncrypt 후 tinkDecrypt 라운드트립`() {
        val plaintext = "확장 함수로 암호화"
        val encrypted = plaintext.tinkEncrypt(aead)

        encrypted shouldNotBeEqualTo plaintext
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @Test
    fun `associatedData를 포함한 ByteArray 라운드트립`() {
        val plaintext = "데이터".toByteArray()
        val ad = "context".toByteArray()

        val encrypted = plaintext.tinkEncrypt(aead, ad)
        encrypted.tinkDecrypt(aead, ad) shouldBeEqualTo plaintext
    }

    @Test
    fun `associatedData를 포함한 String 라운드트립`() {
        val plaintext = "문자열 데이터"
        val ad = "user-id=123".toByteArray()

        val encrypted = plaintext.tinkEncrypt(aead, ad)
        encrypted.tinkDecrypt(aead, ad) shouldBeEqualTo plaintext
    }

    @Test
    fun `ChaCha20-Poly1305로 String 라운드트립`() {
        val aead = TinkAeads.CHACHA20_POLY1305
        val plaintext = "ChaCha20 암호화 테스트"
        val encrypted = plaintext.tinkEncrypt(aead)
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @Test
    fun `XChaCha20-Poly1305로 String 라운드트립`() {
        val aead = TinkAeads.XCHACHA20_POLY1305
        val plaintext = "XChaCha20 암호화 테스트"
        val encrypted = plaintext.tinkEncrypt(aead)
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }
}
