package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.registBouncCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.LINE_SEPARATOR
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.security.Security

class EncryptorsTest {

    companion object: KLogging()

    @BeforeAll
    fun setup() {
        registBouncCastleProvider()
    }

    @Test
    fun `get all pbe algorithms`() {
        val algorithms = Encryptors.getAlgorithmes()
        log.info { algorithms.joinToString(LINE_SEPARATOR) }

        algorithms shouldContain AES.ALGORITHM.uppercase()
        algorithms shouldContain DES.ALGORITHM.uppercase()
        algorithms shouldContain RC2.ALGORITHM.uppercase()
        algorithms shouldContain RC4.ALGORITHM.uppercase()
        algorithms shouldContain TripleDES.ALGORITHM.uppercase()
    }

    @Test
    fun `get all cipher algorithms`() {
        val algorithms = Security.getAlgorithms("Cipher")
        log.info { algorithms.joinToString(LINE_SEPARATOR) }
    }

    @Test
    fun `get security providers`() {
        val providers = Security.getProviders()
        log.info { providers.joinToString(LINE_SEPARATOR) }
    }
}
