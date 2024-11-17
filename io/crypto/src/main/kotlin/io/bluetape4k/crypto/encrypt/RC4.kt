package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * RC4 대칭형 알고리즘을 이용한 [Encryptor] 입니다.
 *
 * @param saltGenerator salt generator
 * @param password password
 */
class RC4(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
    password: String = DEFAULT_PASSWORD,
): AbstractEncryptor(ALGORITHM, saltGenerator, password) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWITHSHA1ANDRC4_128" // "PBEWITHSHAAND128BITRC4"
    }
}
