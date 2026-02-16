package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA-1 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 160비트(20바이트) 해시 값을 생성합니다.
 *
 * **보안 경고**: SHA-1은 충돌 공격에 취약하여 보안 용도로는 권장되지 않습니다.
 * 레거시 호환성이 필요한 경우에만 사용하고, 새로운 개발에서는 [SHA256] 이상을 사용하세요.
 *
 * ```
 * val digester = SHA1()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.SHA1
 * @see SHA256
 */
class SHA1(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-1"
    }
}
