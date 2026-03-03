package io.bluetape4k.crypto.digest

import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.logging.KLogging
import org.jasypt.registry.AlgorithmRegistry

/**
 * 지원되는 모든 해시 다이제스트 알고리즘의 [Digester] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * ## 동작/계약
 * - 각 다이제스터 인스턴스는 lazy 초기화되어 최초 접근 시 생성됩니다.
 * - 초기화 시 BouncyCastle provider 등록을 보장합니다.
 * - 각 프로퍼티는 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val digest = Digesters.SHA256.digest("Hello, World!")
 * val ok = Digesters.SHA256.matches("Hello, World!", digest)
 * // ok == true
 * ```
 */
object Digesters: KLogging() {

    init {
        registerBouncyCastleProvider()
    }

    /**
     * BouncyCastle 프로바이더에서 지원하는 모든 다이제스트 알고리즘 목록을 반환합니다.
     *
     * ## 동작/계약
     * - provider에 등록된 다이제스트 알고리즘 이름 집합을 반환합니다.
     * - 반환 집합의 순서는 보장하지 않습니다.
     *
     * ```kotlin
     * val algorithms = Digesters.getAllDigestAlgorithms()
     * // algorithms.contains("SHA-256") == true
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun getAllDigestAlgorithms(): Set<String> {
        return AlgorithmRegistry.getAllDigestAlgorithms() as Set<String>
    }

    /** KECCAK-256 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK256 by lazy { Keccak256() }

    /** KECCAK-384 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK384 by lazy { Keccak384() }

    /** KECCAK-512 알고리즘을 사용하는 [Digester] 인스턴스 */
    val KECCAK512 by lazy { Keccak512() }

    /** MD5 알고리즘을 사용하는 [Digester] 인스턴스. 보안 용도로는 권장하지 않습니다. */
    val MD5 by lazy { MD5() }

    /** SHA-1 알고리즘을 사용하는 [Digester] 인스턴스. 보안 용도로는 권장하지 않습니다. */
    val SHA1 by lazy { SHA1() }

    /** SHA-256 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA256 by lazy { SHA256() }

    /** SHA-384 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA384 by lazy { SHA384() }

    /** SHA-512 알고리즘을 사용하는 [Digester] 인스턴스 */
    val SHA512 by lazy { SHA512() }

}
