package io.bluetape4k.crypto.encrypt

import io.bluetape4k.logging.KLogging
import org.jasypt.iv.IvGenerator
import org.jasypt.salt.SaltGenerator

/**
 * RC4-128 대칭형 스트림 암호화 알고리즘을 이용한 [Encryptor] 구현체입니다.
 *
 * PBEWITHSHA1ANDRC4_128 알고리즘을 사용합니다.
 * RC4는 스트림 암호로서 IV(초기화 벡터)가 필요하지 않지만,
 * Jasypt 호환성을 위해 IV 생성기 파라미터를 유지합니다.
 *
 * **보안 경고**: RC4는 다수의 알려진 취약점이 있어 TLS 등에서 사용이 금지되었습니다.
 * 레거시 시스템 호환이 필요한 경우에만 사용하고, 새로운 개발에서는 [AES]를 사용하세요.
 *
 * ```
 * val encryptor = RC4()
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param saltGenerator Salt 생성기
 * @param password 암호화/복호화에 사용할 비밀번호
 * @param ivGenerator IV 생성기 (RC4에서는 실질적으로 사용되지 않음)
 * @see Encryptors.RC4
 * @see AES
 */
class RC4(
    saltGenerator: SaltGenerator = DefaultSaltGenerator,
    password: String = DEFAULT_PASSWORD,
    ivGenerator: IvGenerator = DefaultIvGenerator,
): AbstractEncryptor(ALGORITHM, saltGenerator, password, ivGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWITHSHA1ANDRC4_128"
    }
}
