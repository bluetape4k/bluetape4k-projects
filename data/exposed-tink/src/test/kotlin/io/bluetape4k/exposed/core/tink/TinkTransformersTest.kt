package io.bluetape4k.exposed.core.tink

import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.daead.TinkDaeads
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `AEAD 바이너리 transformer 는 동일 입력에 대해 매번 다른 암호문을 생성한다`() {
        val transformer = ByteArrayTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = "non-deterministic-binary-source".toByteArray()

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        // AEAD는 비결정적이므로 다른 암호문 생성
        encrypted1 shouldNotBeEqualTo encrypted2
    }

    @Test
    fun `AEAD Blob transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = TinkAeadBlobTransformer(TinkAeads.AES256_GCM)
        val source = "tink-aead-blob-source".toByteArray()

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `DAEAD Blob transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = TinkDaeadBlobTransformer(TinkDaeads.AES256_SIV)
        val source = "tink-daead-blob-source".toByteArray()

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `DAEAD Blob transformer 는 동일 입력에 대해 항상 같은 암호문을 생성한다`() {
        val transformer = TinkDaeadBlobTransformer(TinkDaeads.AES256_SIV)
        val source = "deterministic-blob-source".toByteArray()

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        // DAEAD는 결정적이므로 항상 동일한 암호문 생성
        encrypted1.bytes shouldBeEqualTo encrypted2.bytes
    }

    // ── 누락 테스트 추가 ───────────────────────────────────────────────────────

    @Test
    fun `AEAD Blob transformer 는 동일 입력에 대해 매번 다른 암호문을 생성한다`() {
        // AEAD(비결정적)이므로 동일 평문이라도 암호문이 달라야 한다.
        val transformer = TinkAeadBlobTransformer(TinkAeads.AES256_GCM)
        val source = "non-deterministic-blob-source".toByteArray()

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        encrypted1.bytes shouldNotBeEqualTo encrypted2.bytes
    }

    @Test
    fun `AEAD 문자열 transformer 는 빈 문자열도 암복호화 round-trip 을 보장한다`() {
        // 빈 문자열(경계값)도 정상 처리되어야 한다.
        val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = ""

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `AEAD 바이너리 transformer 는 빈 바이트 배열도 암복호화 round-trip 을 보장한다`() {
        // 빈 배열(경계값)도 정상 처리되어야 한다.
        val transformer = ByteArrayTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val source = ByteArray(0)

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `AEAD 문자열 transformer 는 훼손된 암호문 복호화 시 예외를 던진다`() {
        // 무결성 검증 실패 시 Tink GeneralSecurityException 이 전파되어야 한다.
        val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        val garbage = "AAAA_invalid_ciphertext_BBBB"

        assertThrows<Exception> {
            transformer.wrap(garbage)
        }
    }

    @Test
    fun `DAEAD 문자열 transformer 는 훼손된 암호문 복호화 시 예외를 던진다`() {
        // DAEAD 역시 잘못된 암호문에 대해 예외를 전파해야 한다.
        val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val garbage = "AAAA_invalid_ciphertext_BBBB"

        assertThrows<Exception> {
            transformer.wrap(garbage)
        }
    }

    @Test
    fun `서로 다른 키로 생성된 AEAD transformer 는 교차 복호화가 불가능하다`() {
        // 서로 다른 키셋으로 초기화된 transformer 간에는 복호화가 실패해야 한다.
        val transformer1 = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
        // AES128_GCM 은 별도 키를 사용하므로 교차 복호화 불가
        val transformer2 = StringTinkAeadEncryptionTransformer(TinkAeads.AES128_GCM)
        val source = "cross-key-test"

        val encrypted = transformer1.unwrap(source)

        assertThrows<Exception> {
            transformer2.wrap(encrypted)
        }
    }

    @Test
    fun `DAEAD 바이너리 transformer 는 빈 배열도 암복호화 round-trip 을 보장한다`() {
        // 빈 배열(경계값)을 결정적으로 암복호화할 수 있어야 한다.
        val transformer = ByteArrayTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
        val source = ByteArray(0)

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }
}
