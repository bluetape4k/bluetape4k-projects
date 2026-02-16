package io.bluetape4k.crypto.cipher

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

class CipherExtensionsTest: AbstractCipherTest() {

    companion object: KLogging()

    @RepeatedTest(REPEAT_SIZE)
    fun `encrypt and decrypt byte array`() {
        val content = Fakers.randomString(128, 1024)
        val plain = content.toUtf8Bytes()

        val encrypted = encryptCipher.encrypt(plain)
        val decrypted = decryptCipher.decrypt(encrypted)

        decrypted.toUtf8String() shouldBeEqualTo content
    }

    @Test
    fun `encrypt null returns empty byte array`() {
        val result = encryptCipher.encrypt(null)
        result shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `encrypt empty byte array returns empty byte array`() {
        val result = encryptCipher.encrypt(emptyByteArray)
        result shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `decrypt null returns empty byte array`() {
        val result = decryptCipher.decrypt(null)
        result shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `decrypt empty byte array returns empty byte array`() {
        val result = decryptCipher.decrypt(emptyByteArray)
        result shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `encrypt with offset and length`() {
        val content = "Hello, World!"
        val plain = content.toUtf8Bytes()

        val encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
        val decryptCipher = builder.build(Cipher.DECRYPT_MODE)

        // "Hello" 부분만 암호화 (offset=0, length=5)
        val encrypted = encryptCipher.encrypt(plain, 0, 5)
        val decrypted = decryptCipher.decrypt(encrypted)

        decrypted.toUtf8String() shouldBeEqualTo "Hello"
    }
}
