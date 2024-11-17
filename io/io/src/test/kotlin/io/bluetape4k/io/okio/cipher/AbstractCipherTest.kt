package io.bluetape4k.io.okio.cipher

import io.bluetape4k.crypto.cipher.CipherBuilder
import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.BeforeEach
import javax.crypto.Cipher

abstract class AbstractCipherTest: AbstractOkioTest() {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5
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
