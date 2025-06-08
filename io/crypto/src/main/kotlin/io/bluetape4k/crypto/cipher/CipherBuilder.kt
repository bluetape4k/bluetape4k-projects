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
 * 암호화/복호화를 위한 [Cipher]를 빌드합니다.
 *
 * 참고: [Java Cipher](https://velog.io/@with667800/Java-Cipher)
 *
 * ```
 * // Build an AES cipher for encryption
 * val cipher = CipherBuilder()
 *    .secretKeySize(16)
 *    .ivBytesSize(16)
 *    .algorithm("AES")
 *    .transformation("AES/CBC/PKCS5Padding")
 *    .build(Cipher.ENCRYPT_MODE)
 * ```
 *
 * ```
 * // Build an AES cipher for decryption
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
        const val DEFAULT_KEY_SIZE = 16
        const val DEFAULT_ALGORITHM = "AES"
        const val DEFAULT_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }

    private val random: Random = secureRandom

    private var algorithm: String = DEFAULT_ALGORITHM
    private var transformation: String = DEFAULT_TRANSFORMATION
    private var secretKey: ByteArray = ByteArray(DEFAULT_KEY_SIZE)
    private var ivBytes: ByteArray = ByteArray(DEFAULT_KEY_SIZE)

    fun secretKeySize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        secretKey = ByteArray(size).also { random.nextBytes(it) }
    }

    fun secretKey(key: ByteArray) = apply {
        secretKey = key
    }

    fun ivBytesSize(size: Int = DEFAULT_KEY_SIZE) = apply {
        size.requirePositiveNumber("size")
        ivBytes = ByteArray(size).also { random.nextBytes(it) }
    }

    fun ivBytes(ivBytes: ByteArray) = apply {
        this.ivBytes = ivBytes
    }

    fun algorithm(algorithm: String = DEFAULT_ALGORITHM) = apply {
        algorithm.requireNotEmpty("algorithm")
        this.algorithm = algorithm
    }

    fun transformation(transformation: String = DEFAULT_TRANSFORMATION) = apply {
        transformation.requireNotEmpty("transformation")
        this.transformation = transformation
    }

    private fun getSecretKeySpec(): SecretKeySpec = SecretKeySpec(secretKey, algorithm)
    private fun getIv(): IvParameterSpec = IvParameterSpec(ivBytes)

    /**
     * 암호화/복호화용 Cipher를 생성합니다.
     *
     * ```
     * // Build an AES cipher for encryption
     * val cipher = CipherBuilder()
     *    .secretKeySize(16)
     *    .ivBytesSize(16)
     *    .algorithm("AES")
     *    .transformation("AES/CBC/PKCS5Padding")
     *    .build(Cipher.ENCRYPT_MODE)
     * ```
     *
     * ```
     * // Build an AES cipher for decryption
     * val decipher = CipherBuilder()
     *      .secretKeySize(16)
     *      .ivBytesSize(16)
     *      .algorithm("AES")
     *      .transformation("AES/CBC/PKCS5Padding")
     *      .build(Cipher.DECRYPT_MODE)
     * ```
     */
    fun build(cipherMode: Int): Cipher {
        return Cipher.getInstance(transformation).also {
            it.init(cipherMode, getSecretKeySpec(), getIv())
        }
    }
}
