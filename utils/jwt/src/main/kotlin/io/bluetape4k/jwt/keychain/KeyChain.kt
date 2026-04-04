package io.bluetape4k.jwt.keychain

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.jwt.JwtConsts.DEFAULT_KEY_ROTATION_TTL_MILLIS
import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.JwtConsts.RSA_ALGORITHM_IDS
import io.bluetape4k.support.hashOf
import io.jsonwebtoken.security.SignatureAlgorithm
import java.security.KeyPair
import java.time.Duration

/**
 * JWT 서명용 키쌍과 메타데이터를 보관하는 키체인입니다.
 *
 * ## 동작/계약
 * - 기본 생성은 RSA 계열 서명 알고리즘만 허용하며, 아니면 [IllegalArgumentException]이 발생합니다.
 * - 기본 `id`는 시간기반 UUID Base62 문자열을 사용합니다.
 * - [isExpired]는 `expiredTtl > 0`일 때만 만료 시점을 검사합니다.
 *
 * ```kotlin
 * val keyChain = KeyChain()
 * // keyChain.algorithm.id in JwtConsts.RSA_ALGORITHM_IDS
 * // keyChain.id.isNotBlank() == true
 * ```
 */
class KeyChain private constructor(
    val algorithm: SignatureAlgorithm,
    val keyPair: KeyPair,
    val id: String,
    val createdAt: Long,
    val expiredTtl: Long,
) : AbstractValueObject() {
    companion object {
        private const val TRANSFORMATION = "RSA"

        /**
         * 새 [KeyChain]을 생성합니다.
         *
         * ## 동작/계약
         * - [algorithm]은 RSA 서명 알고리즘이어야 합니다.
         * - [expiredTtl]은 내부적으로 밀리초로 저장됩니다.
         *
         * @throws IllegalArgumentException RSA 계열 알고리즘이 아닌 경우
         *
         * ```kotlin
         * val keyChain = KeyChain()
         * // keyChain.expiredTtl > 0
         * ```
         */
        @JvmStatic
        operator fun invoke(
            algorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyPair: KeyPair = algorithm.keyPair().build(),
            id: String = Uuid.V7.nextIdAsString(),
            createdAt: Long = System.currentTimeMillis(),
            expiredTtl: Duration = Duration.ofMillis(DEFAULT_KEY_ROTATION_TTL_MILLIS),
        ): KeyChain {
            require(
                algorithm.id in RSA_ALGORITHM_IDS
            ) { "Algorithm must be RSA signature algorithm. got=${algorithm.id}" }
            return KeyChain(algorithm, keyPair, id, createdAt, expiredTtl.toMillis())
        }
    }

    /**
     * 키체인 만료 시각(epoch millis)입니다.
     *
     * ```kotlin
     * val keyChain = KeyChain()
     * val expiredAt = keyChain.expiredAt
     * // expiredAt > System.currentTimeMillis()
     * ```
     */
    val expiredAt: Long
        get() = createdAt + expiredTtl

    /**
     * 현재 시각 기준 만료 여부입니다.
     *
     * ```kotlin
     * val keyChain = KeyChain()
     * val expired = keyChain.isExpired
     * // expired == false  (기본 TTL: 365일)
     * ```
     */
    val isExpired: Boolean
        get() = expiredTtl > 0 && expiredAt < System.currentTimeMillis()

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(id, algorithm, keyPair.private, keyPair.public)

    override fun equalProperties(other: Any): Boolean =
        other is KeyChain &&
            id == other.id &&
            algorithm == other.algorithm &&
            keyPair.private == other.keyPair.private &&
            keyPair.public == other.keyPair.public

    override fun buildStringHelper(): ToStringBuilder =
        super
            .buildStringHelper()
            .add("id", id)
            .add("algorithm", algorithm.id)
            .add("createdAt", createdAt)
}
