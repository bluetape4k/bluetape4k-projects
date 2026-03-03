package io.bluetape4k.crypto.encrypt

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.crypto.encrypt.AbstractEncryptor.Companion.DefaultIvGenerator
import io.bluetape4k.crypto.encrypt.AbstractEncryptor.Companion.DefaultSaltGenerator
import io.bluetape4k.crypto.registerBouncyCastleProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.utils.Runtimex
import org.jasypt.encryption.pbe.PooledPBEByteEncryptor
import org.jasypt.iv.IvGenerator
import org.jasypt.iv.RandomIvGenerator
import org.jasypt.salt.RandomSaltGenerator
import org.jasypt.salt.SaltGenerator

/**
 * [Encryptor] 인터페이스의 추상 구현 클래스입니다.
 *
 * Jasypt의 [PooledPBEByteEncryptor]를 내부적으로 사용하며,
 * 풀 크기를 `2 * 가용 CPU 수`로 구성하여 멀티스레드 환경에서 높은 처리량을 보장합니다.
 *
 * ```
 * val encryptor = AES()
 * val encrypted = encryptor.encrypt("Hello, World!")
 * val decrypted = encryptor.decrypt(encrypted)  // "Hello, World!"
 * ```
 *
 * @param algorithm PBE(Password Based Encryption) 알고리즘 명
 * @param saltGenerator Salt 생성기 (기본값: [DefaultSaltGenerator])
 * @param password 암호화/복호화에 사용할 비밀번호 (기본값: [DEFAULT_PASSWORD])
 * @param ivGenerator IV(초기화 벡터) 생성기 (기본값: [DefaultIvGenerator])
 */
abstract class AbstractEncryptor protected constructor(
    override val algorithm: String,
    override val saltGenerator: SaltGenerator = DefaultSaltGenerator,
    internal val password: String = DEFAULT_PASSWORD,
    private val ivGenerator: IvGenerator = DefaultIvGenerator,
): Encryptor {

    protected companion object: KLogging() {
        /**
         * 랜덤 IV를 생성하는 [RandomIvGenerator] 인스턴스.
         * 암호화 시마다 새로운 IV를 생성하여 결정적 암호화(deterministic encryption)를 방지합니다.
         */
        @JvmStatic
        val DefaultIvGenerator: IvGenerator = RandomIvGenerator()

        /**
         * 랜덤 Salt를 생성하는 [RandomSaltGenerator] 인스턴스.
         * 암호화 시마다 새로운 Salt를 생성하여 Rainbow Table 공격을 방지합니다.
         */
        @JvmStatic
        val DefaultSaltGenerator: SaltGenerator = RandomSaltGenerator()
    }

    /**
     * 내부적으로 사용하는 [PooledPBEByteEncryptor] 인스턴스입니다.
     * lazy 초기화되며, 풀 크기는 `2 * 가용 CPU 수`입니다.
     */
    protected val encryptor: PooledPBEByteEncryptor by lazy {
        PooledPBEByteEncryptor().apply {
            registerBouncyCastleProvider()
            setPoolSize(2 * Runtimex.availableProcessors)
            setAlgorithm(algorithm)
            setIvGenerator(ivGenerator)
            setSaltGenerator(saltGenerator)
            setKeyObtentionIterations(100000)
            setPassword(password)
        }
    }

    /**
     * 지정된 일반 바이트 배열 정보를 암호화하여 바이트 배열로 반환합니다.
     *
     * @param message 일반 바이트 배열 (null이면 빈 배열 반환)
     * @return 암호화된 바이트 배열
     */
    override fun encrypt(message: ByteArray?): ByteArray {
        return message?.let { encryptor.encrypt(it) } ?: emptyByteArray
    }

    /**
     * 암호화된 바이트 배열을 복호화하여, 일반 바이트 배열로 반환합니다.
     *
     * @param encrypted 암호화된 바이트 배열 (null이면 빈 배열 반환)
     * @return 복호화한 바이트 배열
     */
    override fun decrypt(encrypted: ByteArray?): ByteArray {
        return encrypted?.let { encryptor.decrypt(it) } ?: emptyByteArray
    }

    override fun toString(): String = ToStringBuilder(this)
        .add("algorithm", algorithm)
        .toString()
}
