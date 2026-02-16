package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * SHA-256 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 256비트(32바이트) 해시 값을 생성하며, 데이터 무결성 검증, 비밀번호 해싱 등에 널리 사용됩니다.
 * 현재까지 충돌 공격에 대한 실질적인 취약점이 발견되지 않아 보안 용도로 권장됩니다.
 *
 * ```
 * val digester = SHA256()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.SHA256
 */
class SHA256(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "SHA-256"
    }
}
