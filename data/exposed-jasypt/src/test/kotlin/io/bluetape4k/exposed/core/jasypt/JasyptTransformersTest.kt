package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class JasyptTransformersTest {

    @Test
    fun `문자열 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = StringJasyptEncryptionTransformer(Encryptors.AES)
        val source = "jasypt-string-source"

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `바이너리 transformer 는 암복호화 round-trip 을 보장한다`() {
        val transformer = ByteArrayJasyptEncryptionTransformer(Encryptors.RC4)
        val source = "jasypt-binary-source".toUtf8Bytes()

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `동일 입력에 대해 문자열 transformer 는 동일한 암호문을 생성한다`() {
        val transformer = StringJasyptEncryptionTransformer(Encryptors.AES)
        val source = "deterministic-source"

        val encrypted1 = transformer.unwrap(source)
        val encrypted2 = transformer.unwrap(source)

        encrypted1 shouldBeEqualTo encrypted2
    }
}
