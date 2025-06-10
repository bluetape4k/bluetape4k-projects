package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.registBouncCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import org.jasypt.registry.AlgorithmRegistry

/**
 * Hash 알고리즘을 활용한 Digesters
 */
object Digesters: KLogging() {

    init {
        registBouncCastleProvider()
    }

    @Suppress("UNCHECKED_CAST")
    fun getAllDigestAlgorithms(): Set<String> {
        return AlgorithmRegistry.getAllDigestAlgorithms() as Set<String>
    }

    /**
     * Keccak 256 알고리즘을 활용한 Digester
     */
    val KECCAK256 by unsafeLazy { Keccak256() }

    /**
     * Keccak 384 알고리즘을 활용한 Digester
     */
    val KECCAK384 by unsafeLazy { Keccak384() }

    /**
     * Keccak 512 알고리즘을 활용한 Digester
     */
    val KECCAK512 by unsafeLazy { Keccak512() }

    /**
     * MD5 알고리즘을 활용한 Digester
     */
    val MD5 by unsafeLazy { MD5() }

    /**
     * SHA-1 알고리즘을 활용한 Digester
     */
    val SHA1 by unsafeLazy { SHA1() }

    /**
     * SHA-256 알고리즘을 활용한 Digester
     */
    val SHA256 by unsafeLazy { SHA256() }

    /**
     * SHA-384 알고리즘을 활용한 Digester
     */
    val SHA384 by unsafeLazy { SHA384() }

    /**
     * SHA-512 알고리즘을 활용한 Digester
     */
    val SHA512 by unsafeLazy { SHA512() }

}
