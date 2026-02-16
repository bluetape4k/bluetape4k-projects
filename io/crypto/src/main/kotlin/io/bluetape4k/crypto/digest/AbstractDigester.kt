package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.crypto.zeroSaltGenerator
import org.jasypt.digest.PooledByteDigester
import org.jasypt.salt.SaltGenerator

/**
 * [Digester] 인터페이스의 추상 구현 클래스입니다.
 *
 * Jasypt의 [PooledByteDigester]를 내부적으로 사용하며, 풀 크기 8로 구성하여
 * 멀티스레드 환경에서 안전하게 동작합니다.
 *
 * ```
 * val digester = SHA256()
 * val message = "Hello, World!"
 * val digest = digester.digest(message)
 * val matches = digester.matches("Hello, World!", digest)  // true
 * ```
 *
 * @param algorithm Digest 암호화를 위한 알고리즘 명 (예: "SHA-256", "MD5")
 * @param saltGenerator 암호화 시에 사용하는 Salt 값 생성기 (기본: [zeroSaltGenerator])
 */
abstract class AbstractDigester protected constructor(
    override val algorithm: String,
    override val saltGenerator: SaltGenerator = zeroSaltGenerator,
): Digester {

    private val delegate: PooledByteDigester by lazy {
        PooledByteDigester().apply {
            registerBouncyCastleProvider()
            setPoolSize(8)
            setAlgorithm(algorithm)
            setSaltGenerator(saltGenerator)
        }
    }

    /**
     * 바이트 배열 정보를 해시 다이제스트합니다.
     *
     * @param message 다이제스트할 바이트 배열
     * @return 다이제스트된 바이트 배열
     */
    override fun digest(message: ByteArray): ByteArray =
        delegate.digest(message)

    /**
     * 메시지가 다이제스트된 값과 일치하는지 확인합니다.
     *
     * @param message 원본 바이트 배열
     * @param digest 다이제스트된 바이트 배열
     * @return 일치 여부
     */
    override fun matches(message: ByteArray, digest: ByteArray): Boolean =
        delegate.matches(message, digest)

    override fun toString(): String {
        return "${javaClass.simpleName}(algorithm=$algorithm)"
    }
}
