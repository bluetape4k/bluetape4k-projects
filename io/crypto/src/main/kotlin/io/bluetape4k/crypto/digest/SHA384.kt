package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA-384 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 384비트(48바이트) 해시 값을 생성하며, SHA-512의 축약 버전입니다.
 * SHA-256보다 높은 보안 수준이 필요할 때 사용합니다.
 *
 * ```
 * val digester = SHA384()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.SHA384
 */
class SHA384(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-384"
    }
}
