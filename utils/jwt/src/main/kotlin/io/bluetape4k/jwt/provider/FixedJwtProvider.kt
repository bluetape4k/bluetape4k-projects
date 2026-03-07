package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureAlgorithm
import java.security.KeyPair
import java.time.Duration

/**
 * 고정된 키체인을 사용하는 [JwtProvider] 구현체입니다.
 *
 * ## 동작/계약
 * - 키 로테이션을 지원하지 않으며 호출 시 [UnsupportedJwtException]이 발생합니다.
 * - 테스트나 단일 키 환경에 적합합니다.
 *
 * @property current 고정 사용할 [KeyChain]
 */
class FixedJwtProvider private constructor(
    private val current: KeyChain,
): AbstractJwtProvider() {

    companion object: KLogging() {
        /**
         * [FixedJwtProvider] 인스턴스를 생성합니다.
         *
         * @param signatureAlgorithm 서명 알고리즘 (기본: RS256)
         * @param keyPair 서명에 사용할 키쌍
         * @param kid 키체인 식별자
         * @return [FixedJwtProvider] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyPair: KeyPair = signatureAlgorithm.keyPair().build(),
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
