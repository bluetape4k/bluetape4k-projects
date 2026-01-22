package io.bluetape4k.jwt.keychain

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.jwt.JwtConsts.DEFAULT_KEY_ROTATION_TTL_MILLIS
import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.support.hashOf
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.security.KeyPair
import java.time.Duration


class KeyChain private constructor(
    val algorithm: SignatureAlgorithm,
    val keyPair: KeyPair,
    val id: String,
    val createdAt: Long,
    val expiredTtl: Long,
): AbstractValueObject() {

    companion object {
        private const val TRANSFORMATION = "RSA"

        @JvmStatic
        operator fun invoke(
            algorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyPair: KeyPair = Keys.keyPairFor(algorithm),
            id: String = TimebasedUuid.Reordered.nextIdAsString(),
            createdAt: Long = System.currentTimeMillis(),
            expiredTtl: Duration = Duration.ofMillis(DEFAULT_KEY_ROTATION_TTL_MILLIS),
        ): KeyChain {
            require(algorithm.isRsa) { "Algorithm must be RSA signature algorithm." }
            return KeyChain(algorithm, keyPair, id, createdAt, expiredTtl.toMillis())
        }
    }

    val expiredAt: Long
        get() = createdAt + expiredTtl

    val isExpired: Boolean
        get() = expiredTtl > 0 && expiredAt < System.currentTimeMillis()

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(id, algorithm, keyPair.private, keyPair.public)

    override fun equalProperties(other: Any): Boolean {
        return other is KeyChain &&
                id == other.id &&
                algorithm == other.algorithm &&
                keyPair.private == other.keyPair.private &&
                keyPair.public == other.keyPair.public
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
            .add("algorithm", algorithm)
            .add("createdAt", createdAt)
    }
}
