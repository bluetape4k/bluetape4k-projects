package io.bluetape4k.crypto.encrypt

import io.bluetape4k.logging.KLogging
import org.jasypt.iv.IvGenerator
import org.jasypt.salt.SaltGenerator

/**
 * RC2-128 대칭형 암호화 알고리즘을 이용한 [Encryptor] 구현체입니다.
 *
 * PBEWITHSHA1ANDRC2_128 알고리즘을 사용합니다.
 *
 * **보안 경고**: RC2는 현대 보안 표준에 적합하지 않습니다.
 * 레거시 시스템 호환이 필요한 경우에만 사용하고, 새로운 개발에서는 [AES]를 사용하세요.
 *
 * ```
 * val encryptor = RC2()
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param saltGenerator Salt 생성기
 * @param password 암호화/복호화에 사용할 비밀번호
 * @param ivGenerator IV 생성기
 * @see Encryptors.RC2
 * @see AES
 */
class RC2(
    saltGenerator: SaltGenerator = DefaultSaltGenerator,
    password: String = DEFAULT_PASSWORD,
    ivGenerator: IvGenerator = DefaultIvGenerator,
): AbstractEncryptor(ALGORITHM, saltGenerator, password, ivGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWITHSHA1ANDRC2_128"
    }
}
