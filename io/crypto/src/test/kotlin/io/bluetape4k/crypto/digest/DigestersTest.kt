package io.bluetape4k.crypto.digest

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.LINE_SEPARATOR
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class DigestersTest {

    companion object: KLogging()

    @Test
    fun `get all digester algorithmes`() {
        val algorithms = Digesters.getAllDigestAlgorithms()
        log.debug { "algorithm:\n" + algorithms.joinToString(LINE_SEPARATOR) }

        algorithms shouldContain Keccak256.ALGORITHM
        algorithms shouldContain Keccak384.ALGORITHM
        algorithms shouldContain Keccak512.ALGORITHM

        algorithms shouldContain MD5.ALGORITHM
        algorithms shouldContain SHA1.ALGORITHM
        algorithms shouldContain "SHA-256" // SHA256.ALGORITHM
        algorithms shouldContain "SHA-384" // SHA384.ALGORITHM
        algorithms shouldContain "SHA-512" // SHA512.ALGORITHM
    }
}
