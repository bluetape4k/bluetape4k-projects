package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * KECCAK-384 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 384비트(48바이트) 해시 값을 생성합니다.
 * Keccak은 SHA-3 표준의 기반 알고리즘이며, BouncyCastle 프로바이더가 필요합니다.
 *
 * ```
 * val digester = Keccak384()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.KECCAK384
 */
class Keccak384(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "KECCAK-384"
    }
}
