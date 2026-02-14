package io.bluetape4k.crypto.encrypt

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.crypto.encrypt.AbstractEncryptor.Companion.DefaultIvGenerator
import io.bluetape4k.crypto.registBouncCastleProvider
import io.bluetape4k.crypto.zeroSaltGenerator
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.utils.Runtimex
import org.jasypt.encryption.pbe.PooledPBEByteEncryptor
import org.jasypt.iv.IvGenerator
import org.jasypt.iv.StringFixedIvGenerator
import org.jasypt.salt.SaltGenerator
import org.jasypt.salt.StringFixedSaltGenerator

/**
 * [Encryptor] 의 추상 클래스입니다.
 *
 * ```
 * val encryptor = AES256Encryptor(saltGenerator = zeroSaltGenerator)
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param algorithm 대칭형 암호화 알고리즘
 * @param saltGenerator Salt 생성기 (기본값: [zeroSaltGenerator])
 * @param password 비밀번호 (기본값: [DEFAULT_PASSWORD])
 * @param ivGenerator IV 생성기 (기본값: [DefaultIvGenerator])
 */
abstract class AbstractEncryptor protected constructor(
    override val algorithm: String,
    override val saltGenerator: SaltGenerator = DefaultSaltGenerator,
    override val password: String = DEFAULT_PASSWORD,
    private val ivGenerator: IvGenerator = DefaultIvGenerator,
): Encryptor {

    protected companion object: KLogging() {
        /**
         * 기본 비밀번호를 이용하는 [StringFixedIvGenerator] 인스턴스
         */
        @JvmStatic
        val DefaultIvGenerator: IvGenerator = StringFixedIvGenerator(DEFAULT_PASSWORD)

        @JvmStatic
        val DefaultSaltGenerator: SaltGenerator = StringFixedSaltGenerator(DEFAULT_PASSWORD)
    }

    val encryptor: PooledPBEByteEncryptor by lazy {
        PooledPBEByteEncryptor().apply {
            registBouncCastleProvider()
            setPoolSize(2 * Runtimex.availableProcessors)
            setAlgorithm(algorithm)
            setIvGenerator(ivGenerator)  // AES 알고리즘에서는 꼭 지정해줘야 한다.
            setSaltGenerator(saltGenerator)
            setPassword(password)
        }
    }

    /**
     * 지정된 일반 바이트 배열 정보를 암호화하여 바이트 배열로 반환합니다.
     * @param message 일반 바이트 배열
     * @return 암호화된 바이트 배열
     */
    override fun encrypt(message: ByteArray?): ByteArray {
        return message?.run { encryptor.encrypt(this) } ?: emptyByteArray
    }

    /**
     * 암호화된 바이트 배열을 복호화하여, 일반 바이트 배열로 반환합니다.
     * @param encrypted 암호화된 바이트 배열
     * @return 복호화한 바이트 배열
     */
    override fun decrypt(encrypted: ByteArray?): ByteArray {
        return encrypted?.run { encryptor.decrypt(this) } ?: emptyByteArray
    }

    /**
     * 암호화 처리 타입 변환을 위한 `toString` 함수를 제공합니다.
     */
    override fun toString(): String = ToStringBuilder(this)
        .add("algorithm", algorithm)
        .toString()
}
