package io.bluetape4k.crypto.encrypt

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.params.provider.FieldSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
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

    @ParameterizedTest(name = "encrypt null byte array by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt null byte array`(encryptor: Encryptor) {
        encryptor.encrypt(null as ByteArray?) shouldBeEqualTo emptyByteArray
        encryptor.decrypt(null as ByteArray?) shouldBeEqualTo emptyByteArray
    }

    @ParameterizedTest(name = "encrypt empty string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt empty string`(encryptor: Encryptor) {
        val encrypted = encryptor.encrypt("")
        encrypted shouldBeEqualTo ""
    }

    @ParameterizedTest(name = "encrypt null string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt null string`(encryptor: Encryptor) {
        encryptor.encrypt(null as String?) shouldBeEqualTo ""
        encryptor.decrypt(null as String?) shouldBeEqualTo ""
    }

    @ParameterizedTest(name = "encrypt string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt string in multi threadings`(encryptor: Encryptor) {
        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val message = getRandomString()

                val encrypted = encryptor.encrypt(message)
                val decrypted = encryptor.decrypt(encrypted)

                decrypted shouldBeEqualTo message
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @ParameterizedTest(name = "encrypt string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt string in virtual threads`(encryptor: Encryptor) {
        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val message = getRandomString()

                val encrypted = encryptor.encrypt(message)
                val decrypted = encryptor.decrypt(encrypted)

                decrypted shouldBeEqualTo message
            }
            .run()
    }

    @ParameterizedTest(name = "encrypt string by {0}")
    @FieldSource("encryptors")
    fun `encrypt and decrypt string in suspended jobs`(encryptor: Encryptor) = runTest {
        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val message = getRandomString()

                val encrypted = encryptor.encrypt(message)
                val decrypted = encryptor.decrypt(encrypted)

                decrypted shouldBeEqualTo message
            }
            .run()
    }
}
