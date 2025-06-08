package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA1 알고리즘을 이용한 [Digester]
 *
 * @param saltGenerator salt generator
 */
class SHA1(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-1"
    }
}
