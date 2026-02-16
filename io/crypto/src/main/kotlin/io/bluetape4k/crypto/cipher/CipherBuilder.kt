package io.bluetape4k.crypto.cipher

import io.bluetape4k.crypto.secureRandom
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.support.requirePositiveNumber
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 암호화/복호화를 위한 [Cipher]를 빌드하는 빌더 클래스입니다.
 *
 * JCA(Java Cryptography Architecture)의 [Cipher] 인스턴스를 편리하게 생성할 수 있도록
 * 빌더 패턴을 제공합니다. 기본값으로 AES/CBC/PKCS5Padding 알고리즘을 사용합니다.
 *
 * 참고: [Java Cipher](https://velog.io/@with667800/Java-Cipher)
 *
 * ```
 * // AES 암호화 Cipher 생성
 * val cipher = CipherBuilder()
 *    .secretKeySize(16)
 *    .ivBytesSize(16)
 *    .algorithm("AES")
 *    .transformation("AES/CBC/PKCS5Padding")
 *    .build(Cipher.ENCRYPT_MODE)
 * ```
 *
 * ```
 * // AES 복호화 Cipher 생성
 * val decipher = CipherBuilder()
 *      .secretKeySize(16)
 *      .ivBytesSize(16)
 *      .algorithm("AES")
 *      .transformation("AES/CBC/PKCS5Padding")
 *      .build(Cipher.DECRYPT_MODE)
 * ```
 */
class CipherBuilder {

    companion object: KLogging() {
        /** 기본 비밀 키 크기 (바이트) */
        const val DEFAULT_KEY_SIZE = 16

        /** 기본 암호화 알고리즘 */
        const val DEFAULT_ALGORITHM = "AES"

        /** 기본 변환 문자열 (알고리즘/모드/패딩) */
        const val DEFAULT_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }

    private val random: Random = secureRandom

    private var algorithm: String = DEFAULT_ALGORITHM
    private var transformation: String = DEFAULT_TRANSFORMATION
    private var secretKey: ByteArray = ByteArray(DEFAULT_KEY_SIZE)
    private var ivBytes: ByteArray = ByteArray(DEFAULT_KEY_SIZE)

    /**
     * 비밀 키의 크기를 지정하고 [SecureRandom]으로 랜덤 키를 생성합니다.
     *
     * @param size 비밀 키 크기 (바이트 단위, 양수여야 함)
     * @return 빌더 인스턴스
     */
    fun secretKeySize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        secretKey = ByteArray(size).also { random.nextBytes(it) }
    }

    /**
     * 비밀 키를 직접 지정합니다.
     *
     * @param key 사용할 비밀 키 바이트 배열
     * @return 빌더 인스턴스
     */
    fun secretKey(key: ByteArray) = apply {
        secretKey = key
    }

    /**
     * IV(초기화 벡터)의 크기를 지정하고 [SecureRandom]으로 랜덤 IV를 생성합니다.
     *
     * CBC 모드 등 블록 암호 모드에서 필요합니다.
     *
     * @param size IV 크기 (바이트 단위, 양수여야 함)
     * @return 빌더 인스턴스
     */
    fun ivBytesSize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        ivBytes = ByteArray(size).also { random.nextBytes(it) }
    }

    /**
     * IV(초기화 벡터)를 직접 지정합니다.
     *
     * @param ivBytes 사용할 IV 바이트 배열
     * @return 빌더 인스턴스
     */
    fun ivBytes(ivBytes: ByteArray) = apply {
        this.ivBytes = ivBytes
    }

    /**
     * 암호화 알고리즘을 지정합니다.
     *
     * [SecretKeySpec] 생성 시 사용됩니다.
     *
     * @param algorithm 암호화 알고리즘 명 (예: "AES", "DES")
     * @return 빌더 인스턴스
     */
    fun algorithm(algorithm: String = DEFAULT_ALGORITHM) = apply {
        algorithm.requireNotEmpty("algorithm")
        this.algorithm = algorithm
    }

    /**
     * 변환(transformation) 문자열을 지정합니다.
     *
     * "알고리즘/모드/패딩" 형식이며, [Cipher.getInstance]에 전달됩니다.
     *
     * @param transformation 변환 문자열 (예: "AES/CBC/PKCS5Padding")
     * @return 빌더 인스턴스
     */
    fun transformation(transformation: String = DEFAULT_TRANSFORMATION) = apply {
        transformation.requireNotEmpty("transformation")
        this.transformation = transformation
    }

    private fun getSecretKeySpec(): SecretKeySpec = SecretKeySpec(secretKey, algorithm)
    private fun getIv(): IvParameterSpec = IvParameterSpec(ivBytes)

    /**
     * 지정된 모드로 [Cipher] 인스턴스를 생성합니다.
     *
     * ```
     * val encryptCipher = CipherBuilder()
     *    .secretKeySize(16)
     *    .ivBytesSize(16)
     *    .build(Cipher.ENCRYPT_MODE)
     *
     * val decryptCipher = CipherBuilder()
     *    .secretKeySize(16)
     *    .ivBytesSize(16)
     *    .build(Cipher.DECRYPT_MODE)
     * ```
     *
     * @param cipherMode [Cipher.ENCRYPT_MODE], [Cipher.DECRYPT_MODE], [Cipher.WRAP_MODE], [Cipher.UNWRAP_MODE] 중 하나
     * @return 초기화된 [Cipher] 인스턴스
     * @throws IllegalArgumentException [cipherMode]가 유효하지 않은 경우
     */
    fun build(cipherMode: Int): Cipher {
        require(cipherMode in 1..4) {
            "cipherMode는 Cipher.ENCRYPT_MODE(1), DECRYPT_MODE(2), WRAP_MODE(3), UNWRAP_MODE(4) 중 하나여야 합니다. cipherMode=$cipherMode"
        }
        return Cipher.getInstance(transformation).also {
            it.init(cipherMode, getSecretKeySpec(), getIv())
        }
    }
}
