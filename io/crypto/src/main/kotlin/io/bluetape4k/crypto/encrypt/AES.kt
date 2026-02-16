package io.bluetape4k.crypto.encrypt

import io.bluetape4k.logging.KLogging
import org.jasypt.iv.IvGenerator
import org.jasypt.salt.SaltGenerator

/**
 * AES-256 대칭형 암호화 알고리즘을 이용한 [Encryptor] 구현체입니다.
 *
 * PBEWITHHMACSHA256ANDAES_256 알고리즘을 사용하며, 현재 가장 널리 사용되는
 * 안전한 대칭형 암호화 방식입니다. 새로운 개발에서 권장됩니다.
 *
 * ```
 * val encryptor = AES()
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param saltGenerator Salt 생성기
 * @param password 암호화/복호화에 사용할 비밀번호
 * @param ivGenerator IV 생성기
 * @see Encryptors.AES
 */
class AES(
    saltGenerator: SaltGenerator = DefaultSaltGenerator,
    password: String = DEFAULT_PASSWORD,
    ivGenerator: IvGenerator = DefaultIvGenerator,
): AbstractEncryptor(ALGORITHM, saltGenerator, password, ivGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWITHHMACSHA256ANDAES_256"
    }
}
