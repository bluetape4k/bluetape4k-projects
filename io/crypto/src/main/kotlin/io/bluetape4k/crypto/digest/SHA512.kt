package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA-512 알고리즘을 이용한 [Digester]
 *
 * @param saltGenerator salt generator
 */
class SHA512(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-512"
    }
}
