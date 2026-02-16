package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA-512 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 512비트(64바이트) 해시 값을 생성하며, SHA-2 계열 중 가장 긴 해시를 제공합니다.
 * 64비트 플랫폼에서 SHA-256보다 오히려 빠를 수 있습니다.
 *
 * ```
 * val digester = SHA512()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.SHA512
 */
class SHA512(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-512"
    }
}
