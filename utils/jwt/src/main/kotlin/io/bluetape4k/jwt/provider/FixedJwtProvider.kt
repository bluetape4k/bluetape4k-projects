package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import java.security.KeyPair
import java.time.Duration

class FixedJwtProvider private constructor(
    private val current: KeyChain,
): AbstractJwtProvider() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyPair: KeyPair = Keys.keyPairFor(signatureAlgorithm),
            kid: String,
        ): FixedJwtProvider {
            val keyChain = KeyChain(signatureAlgorithm, keyPair, kid, expiredTtl = Duration.ofMillis(0))
            return FixedJwtProvider(keyChain)
        }
    }

    override val signatureAlgorithm: SignatureAlgorithm = current.algorithm

    override fun createKeyChain(): KeyChain =
        KeyChain(signatureAlgorithm, expiredTtl = Duration.ofMillis(0))

    override fun currentKeyChain(): KeyChain {
        return current
    }

    override fun rotate(): Boolean {
        throw UnsupportedJwtException("FixedJwtProvider does not support key rotation.")
    }

    override fun forcedRotate(): Boolean {
        throw UnsupportedJwtException("FixedJwtProvider does not support key rotation.")
    }

    override fun findKeyChain(kid: String): KeyChain? {
        return if (current.id == kid) current else null
    }
}
