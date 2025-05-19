package io.bluetape4k.crypto.encrypt

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.params.provider.FieldSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments

class EncryptorTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private fun getRandomString() = Fakers.faker.lorem().paragraph(12)
        // Fakers.randomString(256, 4096)
    }

    private val encryptors: List<Arguments> =
        listOf(
            Encryptors.AES,
            Encryptors.DES,
            Encryptors.RC2,
            Encryptors.RC4,
            Encryptors.TripleDES
        ).map {
            Arguments.of(it)
        }

    @ParameterizedTest(name = "encrypt byte array by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt byte array`(encryptor: Encryptor) {
        repeat(REPEAT_SIZE) {
            val message: ByteArray = getRandomString().toUtf8Bytes()

            val encrypted = encryptor.encrypt(message)
            val decrypted = encryptor.decrypt(encrypted)

            decrypted shouldBeEqualTo message
        }
    }

    @ParameterizedTest(name = "encrypt string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt string`(encryptor: Encryptor) {
        repeat(REPEAT_SIZE) {
            val message = getRandomString()

            val encrypted = encryptor.encrypt(message)
            val decrypted = encryptor.decrypt(encrypted)

            log.debug { "message=$message" }
            log.debug { "encrypted=$encrypted" }
            log.debug { "decrypted=$decrypted" }

            decrypted shouldBeEqualTo message
        }
    }

    @ParameterizedTest(name = "encrypt char array by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt char array`(encryptor: Encryptor) {
        repeat(REPEAT_SIZE) {
            val message: CharArray = getRandomString().toCharArray()

            val encrypted = encryptor.encrypt(message)
            val decrypted = encryptor.decrypt(encrypted)

            decrypted shouldBeEqualTo message
        }
    }
}
