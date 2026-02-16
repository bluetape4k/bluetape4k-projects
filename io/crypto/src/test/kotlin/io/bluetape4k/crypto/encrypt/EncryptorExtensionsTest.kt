package io.bluetape4k.crypto.encrypt

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.params.provider.FieldSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments

class EncryptorExtensionsTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val encryptors: List<Arguments> = listOf(
        Encryptors.AES,
        Encryptors.DES,
        Encryptors.RC2,
        Encryptors.RC4,
        Encryptors.TripleDES,
    ).map { Arguments.of(it) }

    @ParameterizedTest(name = "String.encrypt/decrypt by {0}")
    @FieldSource("encryptors")
    fun `String encrypt and decrypt extension function`(encryptor: Encryptor) {
        repeat(REPEAT_SIZE) {
            val message = Fakers.faker.lorem().paragraph(12)
            val encrypted = message.encrypt(encryptor)
            val decrypted = encrypted.decrypt(encryptor)
            decrypted shouldBeEqualTo message
        }
    }

    @ParameterizedTest(name = "ByteArray.encrypt/decrypt by {0}")
    @FieldSource("encryptors")
    fun `ByteArray encrypt and decrypt extension function`(encryptor: Encryptor) {
        repeat(REPEAT_SIZE) {
            val message = Fakers.faker.lorem().paragraph(12).toUtf8Bytes()
            val encrypted = message.encrypt(encryptor)
            val decrypted = encrypted.decrypt(encryptor)
            decrypted shouldBeEqualTo message
        }
    }
}
