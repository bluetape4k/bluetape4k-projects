package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * KECCAK-512 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 512비트(64바이트) 해시 값을 생성합니다.
 * Keccak 계열 중 가장 긴 해시를 제공하며, BouncyCastle 프로바이더가 필요합니다.
 *
 * ```
 * val digester = Keccak512()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.KECCAK512
 */
class Keccak512(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "KECCAK-512"
    }
}
