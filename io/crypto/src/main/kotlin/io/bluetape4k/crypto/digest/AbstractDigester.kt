package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.registBouncCastleProvider
import io.bluetape4k.crypto.zeroSaltGenerator
import org.jasypt.digest.PooledByteDigester
import org.jasypt.salt.SaltGenerator

/**
 * [Digester]의 최상위 추상화 클래스
 *
 * ```
 * val digester = SHA256Digester()
 * val message = "Hello, World!"
 * val digest = digester.digest(message)
 * val matches = digester.matches("Hello, World!", digest)  // true
 * ```
 *
 * @param algorithm Digest 암호화를 위한 알고리즘 명
 * @param saltGenerator 암호화 시에 사용하는 Salt 값 생성기 (기본: [zeroSaltGenerator])
 */
abstract class AbstractDigester protected constructor(
    override val algorithm: String,
    override val saltGenerator: SaltGenerator = zeroSaltGenerator,
): Digester {

    private val delegate: PooledByteDigester by lazy {
        PooledByteDigester().apply {
            registBouncCastleProvider()
            setPoolSize(8)
            setAlgorithm(algorithm)
            setSaltGenerator(saltGenerator)
        }
    }

    /**
     * 암호화 처리에서 `digest` 함수를 제공합니다.
     */
    override fun digest(message: ByteArray): ByteArray =
        delegate.digest(message)

    /**
     * 암호화 처리에서 `matches` 함수를 제공합니다.
     */
    override fun matches(message: ByteArray, digest: ByteArray): Boolean =
        delegate.matches(message, digest)

    /**
     * 암호화 처리 타입 변환을 위한 `toString` 함수를 제공합니다.
     */
    override fun toString(): String {
        return "${javaClass.simpleName}(algorithm=$algorithm)"
    }
}
