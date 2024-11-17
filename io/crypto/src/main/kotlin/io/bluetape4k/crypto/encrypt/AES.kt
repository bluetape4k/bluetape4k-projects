package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator
import org.jasypt.util.binary.AES256BinaryEncryptor

/**
 * AES 대칭형 알고리즘을 이용한 [Encryptor] 입니다.
 *
 * @param saltGenerator salt generator
 * @param password password
 *
 * @see AES256BinaryEncryptor
 */
class AES(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
    password: String = DEFAULT_PASSWORD,
): AbstractEncryptor(ALGORITHM, saltGenerator, password) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWithHMACSHA512AndAES_256"
    }
}
