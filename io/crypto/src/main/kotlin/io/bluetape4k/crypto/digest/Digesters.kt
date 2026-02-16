package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import org.jasypt.registry.AlgorithmRegistry

/**
 * 지원되는 모든 해시 다이제스트 알고리즘의 [Digester] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며, 스레드 안전합니다.
 *
 * ```
 * // SHA-256 다이제스트 사용
 * val digest = Digesters.SHA256.digest("Hello, World!")
 * val matches = Digesters.SHA256.matches("Hello, World!", digest) // true
 *
 * // 지원되는 모든 알고리즘 조회
 * val algorithms = Digesters.getAllDigestAlgorithms()
 * ```
 */
object Digesters: KLogging() {

    init {
        registerBouncyCastleProvider()
    }

    /**
     * BouncyCastle 프로바이더에서 지원하는 모든 다이제스트 알고리즘 목록을 반환합니다.
     *
     * @return 지원되는 다이제스트 알고리즘 명의 [Set]
     */
    @Suppress("UNCHECKED_CAST")
    fun getAllDigestAlgorithms(): Set<String> {
        return AlgorithmRegistry.getAllDigestAlgorithms() as Set<String>
    }

    /** KECCAK-256 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK256 by unsafeLazy { Keccak256() }

    /** KECCAK-384 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK384 by unsafeLazy { Keccak384() }

    /** KECCAK-512 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK512 by unsafeLazy { Keccak512() }

    /** MD5 알고리즘을 사용하는 [Digester] 인스턴스. 보안 용도로는 권장하지 않습니다. */
    val MD5 by unsafeLazy { MD5() }

    /** SHA-1 알고리즘을 사용하는 [Digester] 인스턴스. 보안 용도로는 권장하지 않습니다. */
    val SHA1 by unsafeLazy { SHA1() }

    /** SHA-256 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA256 by unsafeLazy { SHA256() }

    /** SHA-384 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA384 by unsafeLazy { SHA384() }

    /** SHA-512 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA512 by unsafeLazy { SHA512() }

}
