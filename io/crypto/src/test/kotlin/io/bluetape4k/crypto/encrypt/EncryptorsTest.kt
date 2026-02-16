package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.LINE_SEPARATOR
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.Security

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EncryptorsTest {

    companion object: KLogging()

    @BeforeAll
    fun setup() {
        registerBouncyCastleProvider()
    }

    @Test
    fun `get all pbe algorithms`() {
        val algorithms = Encryptors.getAlgorithms()
        log.debug { algorithms.joinToString(LINE_SEPARATOR) }

        algorithms shouldContain AES.ALGORITHM.uppercase()
        algorithms shouldContain DES.ALGORITHM.uppercase()
        algorithms shouldContain RC2.ALGORITHM.uppercase()
        algorithms shouldContain RC4.ALGORITHM.uppercase()
        algorithms shouldContain TripleDES.ALGORITHM.uppercase()
    }

    @Test
    fun `get all cipher algorithms`() {
        val algorithms = Security.getAlgorithms("Cipher")
        log.debug { algorithms.joinToString(LINE_SEPARATOR) }
    }

    @Test
    fun `get security providers`() {
        val providers = Security.getProviders()
        log.debug { providers.joinToString(LINE_SEPARATOR) }
    }
}
