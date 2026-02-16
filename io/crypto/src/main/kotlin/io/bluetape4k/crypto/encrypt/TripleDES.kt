package io.bluetape4k.crypto.encrypt

import io.bluetape4k.logging.KLogging
import org.jasypt.iv.IvGenerator
import org.jasypt.salt.SaltGenerator

/**
 * TripleDES(3DES) 대칭형 암호화 알고리즘을 이용한 [Encryptor] 구현체입니다.
 *
 * PBEWithMD5AndTripleDES 알고리즘을 사용합니다.
 * DES를 3번 적용하여 보안을 강화한 방식으로, DES보다는 안전하지만
 * AES에 비해 성능과 보안 모두 뒤처집니다.
 *
 * ```
 * val encryptor = TripleDES()
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param saltGenerator Salt 생성기
 * @param password 암호화/복호화에 사용할 비밀번호
 * @param ivGenerator IV 생성기
 * @see Encryptors.TripleDES
 * @see AES
 */
class TripleDES(
    saltGenerator: SaltGenerator = DefaultSaltGenerator,
    password: String = DEFAULT_PASSWORD,
    ivGenerator: IvGenerator = DefaultIvGenerator,
): AbstractEncryptor(ALGORITHM, saltGenerator, password, ivGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "PBEWithMD5AndTripleDES"
    }
}
