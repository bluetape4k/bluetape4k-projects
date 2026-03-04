package io.bluetape4k.exposed.core.tink

import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.daead.TinkDaeads
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class TinkTransformersTest {

    @Test
    fun `AEAD 문자열 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = "tink-aead-string-source"

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `AEAD 바이너리 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = ByteArrayTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = "tink-aead-binary-source".toByteArray()

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `AEAD 문자열 transformer 는 동일 입력에 대해 매번 다른 암호문을 생성한다`() {
        val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = "non-deterministic-source"

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        // AEAD는 비결정적이므로 다른 암호문 생성
        encrypted1 shouldNotBeEqualTo encrypted2
    }

    @Test
    fun `DAEAD 문자열 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val source = "tink-daead-string-source"

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `DAEAD 바이너리 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = ByteArrayTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val source = "tink-daead-binary-source".toByteArray()

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `DAEAD 문자열 transformer 는 동일 입력에 대해 항상 같은 암호문을 생성한다`() {
        val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val source = "deterministic-source"

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        // DAEAD는 결정적이므로 항상 동일한 암호문 생성
        encrypted1 shouldBeEqualTo encrypted2
    }

    @Test
    fun `DAEAD 바이너리 transformer 는 동일 입력에 대해 항상 같은 암호문을 생성한다`() {
        val transformer = ByteArrayTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val source = "deterministic-binary-source".toByteArray()

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        // DAEAD는 결정적이므로 항상 동일한 암호문 생성
        encrypted1 shouldBeEqualTo encrypted2
    }

    @Test
    fun `ChaCha20-Poly1305 AEAD transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.CHACHA20_POLY1305)
        val source = "tink-chacha20-poly1305-source"

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }
}
