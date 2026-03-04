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
 * ## 동작/계약
 * - 기본 알고리즘/변환은 `AES`/`AES/CBC/PKCS5Padding`입니다.
 * - 키/IV를 지정하지 않으면 기본 길이(16) 배열이 사용됩니다.
 * - 빌더 메서드는 내부 상태를 갱신(mutate)하고 `this`를 반환합니다.
 * - [build] 시점에 JCA 초기화 오류가 발생하면 예외가 전파됩니다.
 *
 * ```kotlin
 * val cipher = CipherBuilder().secretKeySize(16).ivBytesSize(16).build(Cipher.ENCRYPT_MODE)
 * val decipher = CipherBuilder().secretKeySize(16).ivBytesSize(16).build(Cipher.DECRYPT_MODE)
 * // cipher.algorithm.contains("AES") == true
 * ```
 */
@Deprecated(message = "io.bluetape4k.tink.aead.TinkAead 또는 JCA Cipher를 직접 사용하세요.")
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
    private var secretKey: ByteArray = ByteArray(DEFAULT_KEY_SIZE).also { random.nextBytes(it) }
    private var ivBytes: ByteArray = ByteArray(DEFAULT_KEY_SIZE).also { random.nextBytes(it) }

    /**
     * 비밀 키의 크기를 지정하고 [SecureRandom]으로 랜덤 키를 생성합니다.
     *
     * ## 동작/계약
     * - [size]는 양수여야 하며, 0 이하이면 예외가 발생합니다.
     * - 길이가 [size]인 새 키 배열을 할당해 내부 상태를 갱신합니다.
     *
     * ```kotlin
     * val builder = CipherBuilder().secretKeySize(32)
     * // builder != null
     * ```
     * @param size 비밀 키 길이(바이트). 0 이하이면 [IllegalArgumentException]이 발생합니다.
     */
    fun secretKeySize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        secretKey = ByteArray(size).also { random.nextBytes(it) }
    }

    /**
     * 비밀 키를 직접 지정합니다.
     *
     * ## 동작/계약
     * - 전달된 [key]를 방어적으로 복사하여 내부 키로 사용합니다.
     * - 호출 후 원본 배열을 수정해도 빌더 내부 키에 영향을 미치지 않습니다.
     *
     * ```kotlin
     * val key = ByteArray(16) { 1 }
     * val builder = CipherBuilder().secretKey(key)
     * // builder != null
     * ```
     * @param key 사용할 비밀 키 바이트 배열
     */
    fun secretKey(key: ByteArray) = apply {
        secretKey = key.copyOf()
    }

    /**
     * IV(초기화 벡터)의 크기를 지정하고 [SecureRandom]으로 랜덤 IV를 생성합니다.
     *
     * ## 동작/계약
     * - [size]는 양수여야 하며, 0 이하이면 예외가 발생합니다.
     * - 길이가 [size]인 새 IV 배열을 할당해 내부 상태를 갱신합니다.
     *
     * ```kotlin
     * val builder = CipherBuilder().ivBytesSize(16)
     * // builder != null
     * ```
     * @param size IV 길이(바이트). 0 이하이면 [IllegalArgumentException]이 발생합니다.
     */
    fun ivBytesSize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        ivBytes = ByteArray(size).also { random.nextBytes(it) }
    }

    /**
     * IV(초기화 벡터)를 직접 지정합니다.
     *
     * ## 동작/계약
     * - 전달된 [ivBytes]를 방어적으로 복사하여 내부 IV로 사용합니다.
     * - 호출 후 원본 배열을 수정해도 빌더 내부 IV에 영향을 미치지 않습니다.
     *
     * ```kotlin
     * val iv = ByteArray(16) { 2 }
     * val builder = CipherBuilder().ivBytes(iv)
     * // builder != null
     * ```
     * @param ivBytes 사용할 IV 바이트 배열
     */
    fun ivBytes(ivBytes: ByteArray) = apply {
        this.ivBytes = ivBytes.copyOf()
    }

    /**
     * 암호화 알고리즘을 지정합니다.
     *
     * ## 동작/계약
     * - [algorithm]은 비어 있으면 안 되며 비어 있으면 예외가 발생합니다.
     * - 지정 값은 [SecretKeySpec] 생성에 사용됩니다.
     *
     * ```kotlin
     * val builder = CipherBuilder().algorithm("AES")
     * // builder != null
     * ```
     * @param algorithm 알고리즘 이름. 빈 문자열이면 [IllegalArgumentException]이 발생합니다.
     */
    fun algorithm(algorithm: String = DEFAULT_ALGORITHM) = apply {
        algorithm.requireNotEmpty("algorithm")
        this.algorithm = algorithm
    }

    /**
     * 변환(transformation) 문자열을 지정합니다.
     *
     * ## 동작/계약
     * - [transformation]은 비어 있으면 안 되며 비어 있으면 예외가 발생합니다.
     * - 지정 값은 [Cipher.getInstance]에 전달됩니다.
     *
     * ```kotlin
     * val builder = CipherBuilder().transformation("AES/CBC/PKCS5Padding")
     * // builder != null
     * ```
     * @param transformation 변환 문자열. 빈 문자열이면 [IllegalArgumentException]이 발생합니다.
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
     * ## 동작/계약
     * - [cipherMode]는 `1..4` 범위(`ENCRYPT/DECRYPT/WRAP/UNWRAP`)여야 합니다.
     * - 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 호출마다 새 [Cipher]를 생성하고 초기화해 반환합니다.
     *
     * ```kotlin
     * val cipher = CipherBuilder().secretKeySize(16).ivBytesSize(16).build(Cipher.ENCRYPT_MODE)
     * // cipher.blockSize > 0
     * ```
     *
     * @param cipherMode 암복호화 모드. `1..4` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
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
