package io.bluetape4k.crypto.cipher

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import javax.crypto.Cipher

abstract class AbstractCipherTest {
    companion object: KLogging() {
        const val REPEAT_SIZE = 10
    }

    protected val builder = CipherBuilder()
        .secretKeySize(16)
        .ivBytesSize(16)
        .algorithm(CipherBuilder.DEFAULT_ALGORITHM)
        .transformation(CipherBuilder.DEFAULT_TRANSFORMATION)

    protected lateinit var encryptCipher: Cipher
    protected lateinit var decryptCipher: Cipher

    @BeforeEach
    fun beforeEach() {
        encryptCipher = builder.build(Cipher.ENCRYPT_MODE)
        decryptCipher = builder.build(Cipher.DECRYPT_MODE)
    }
}
