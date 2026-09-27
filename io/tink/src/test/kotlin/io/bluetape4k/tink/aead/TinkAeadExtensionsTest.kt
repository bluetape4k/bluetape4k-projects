package io.bluetape4k.tink.aead

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.AbstractTinkTest
import org.junit.jupiter.api.RepeatedTest

class TinkAeadExtensionsTest: AbstractTinkTest() {

    companion object: KLogging()

    private val aead = TinkAeads.AES256_GCM

    @RepeatedTest(REPEAT_SIZE)
    fun `ByteArray tinkEncrypt 후 tinkDecrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val encrypted = plaintext.tinkEncrypt(aead)

        encrypted shouldNotBeEqualTo plaintext
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `String tinkEncrypt 후 tinkDecrypt 라운드트립`() {
        val plaintext = faker.lorem().paragraph()
        val encrypted = plaintext.tinkEncrypt(aead)

        encrypted shouldNotBeEqualTo plaintext
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `associatedData를 포함한 ByteArray 라운드트립`() {
        val plaintext = faker.lorem().paragraph().toUtf8Bytes()
        val ad = "context".toUtf8Bytes()

        val encrypted = plaintext.tinkEncrypt(aead, ad)
        encrypted.tinkDecrypt(aead, ad) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `associatedData를 포함한 String 라운드트립`() {
        val plaintext = faker.lorem().paragraph()
        val ad = "user-id=123".toUtf8Bytes()

        val encrypted = plaintext.tinkEncrypt(aead, ad)
        encrypted.tinkDecrypt(aead, ad) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `ChaCha20-Poly1305로 String 라운드트립`() {
        val aead = TinkAeads.CHACHA20_POLY1305
        val plaintext = faker.lorem().paragraph()

        val encrypted = plaintext.tinkEncrypt(aead)
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `XChaCha20-Poly1305로 String 라운드트립`() {
        val aead = TinkAeads.XCHACHA20_POLY1305
        val plaintext = faker.lorem().paragraph()

        val encrypted = plaintext.tinkEncrypt(aead)
        encrypted.tinkDecrypt(aead) shouldBeEqualTo plaintext
    }
}
