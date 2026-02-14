package io.bluetape4k.exposed.core.encrypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class EncryptionTransformersTest {

    @Test
    fun `ByteArray 암호화 transformer 는 원본을 복원한다`() {
        val source = "encrypt-binary-source".toUtf8Bytes()
        val transformer = ByteArrayEncryptionTransformer(Encryptors.AES)

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `String 암호화 transformer 는 원본 문자열을 복원한다`() {
        val source = "encrypt-string-source"
        val transformer = StringEncryptionTransformer(Encryptors.AES)

        val encrypted = transformer.unwrap(source)
        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 암호화 transformer 는 원본 ByteArray 를 복원한다`() {
        val source = "encrypt-blob-source".toUtf8Bytes()
        val transformer = EncryptedBlobTransformer(Encryptors.AES)

        val encryptedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(encryptedBlob)

        restored.toUtf8String() shouldBeEqualTo source.toUtf8String()
    }

    @Test
    fun `Blob 암호화 transformer 는 암호화된 blob 을 복호화할 수 있다`() {
        val source = "encrypt-blob-source".toUtf8Bytes()
        val encrypted = Encryptors.AES.encrypt(source).toExposedBlob()
        val transformer = EncryptedBlobTransformer(Encryptors.AES)

        val restored = transformer.wrap(encrypted)

        restored shouldBeEqualTo source
    }
}
