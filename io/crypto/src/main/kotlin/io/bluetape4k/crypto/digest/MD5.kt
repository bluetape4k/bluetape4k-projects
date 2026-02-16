package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import org.jasypt.salt.SaltGenerator

/**
 * MD5 해시 알고리즘을 이용한 [Digester] 구현체입니다.
 *
 * 128비트(16바이트) 해시 값을 생성합니다.
 *
 * **보안 경고**: MD5는 충돌 공격에 취약하여 보안 용도(비밀번호 해싱, 디지털 서명 등)로는
 * 사용하지 마세요. 체크섬 등 비보안 용도에서만 사용하고,
 * 보안이 필요하면 [SHA256] 이상을 사용하세요.
 *
 * ```
 * val digester = MD5()
 * val digest = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", digest) // true
 * ```
 *
 * @param saltGenerator Salt 값 생성기 (기본: [zeroSaltGenerator])
 * @see Digesters.MD5
 * @see SHA256
 */
class MD5(
    saltGenerator: SaltGenerator = zeroSaltGenerator,
): AbstractDigester(ALGORITHM, saltGenerator) {

    companion object: KLogging() {
        const val ALGORITHM = "MD5"
    }
}
