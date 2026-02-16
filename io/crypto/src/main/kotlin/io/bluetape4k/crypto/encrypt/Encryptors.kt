package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import org.jasypt.registry.AlgorithmRegistry

/**
 * 지원되는 모든 대칭형 암호화 알고리즘의 [Encryptor] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며, 스레드 안전합니다.
 *
 * ```
 * // AES 암호화 사용
 * val encrypted = Encryptors.AES.encrypt("Hello, World!")
 * val decrypted = Encryptors.AES.decrypt(encrypted)  // "Hello, World!"
 *
 * // 지원되는 모든 PBE 알고리즘 조회
 * val algorithms = Encryptors.getAlgorithms()
 * ```
 */
object Encryptors: KLogging() {

    init {
        registerBouncyCastleProvider()
    }

    /** AES-256 알고리즘 (PBEWITHHMACSHA256ANDAES_256)을 이용한 대칭형 암호기. 보안 용도로 권장됩니다. */
    val AES by publicLazy { AES() }

    /** RC2-128 알고리즘 (PBEWITHSHA1ANDRC2_128)을 이용한 대칭형 암호기. 레거시 호환용으로만 사용하세요. */
    val RC2 by publicLazy { RC2() }

    /** RC4-128 알고리즘 (PBEWITHSHA1ANDRC4_128)을 이용한 대칭형 암호기. 레거시 호환용으로만 사용하세요. */
    val RC4 by publicLazy { RC4() }

    /** DES 알고리즘 (PBEWITHMD5ANDDES)을 이용한 대칭형 암호기. 레거시 호환용으로만 사용하세요. */
    val DES by publicLazy { DES() }

    /** TripleDES 알고리즘 (PBEWithMD5AndTripleDES)을 이용한 대칭형 암호기 */
    val TripleDES by publicLazy { TripleDES() }

    /**
     * BouncyCastle 프로바이더에서 지원하는 모든 PBE 알고리즘 목록을 반환합니다.
     *
     * @return 지원되는 PBE 알고리즘 명의 [Set]
     */
    @Suppress("UNCHECKED_CAST")
    fun getAlgorithms(): Set<String> {
        registerBouncyCastleProvider()
        return AlgorithmRegistry.getAllPBEAlgorithms() as Set<String>
    }

    /**
     * [getAlgorithms]의 이전 이름입니다.
     * @see getAlgorithms
     */
    @Deprecated(
        message = "오타 수정. getAlgorithms()를 사용하세요.",
        replaceWith = ReplaceWith("getAlgorithms()"),
    )
    fun getAlgorithmes(): Set<String> = getAlgorithms()
}
