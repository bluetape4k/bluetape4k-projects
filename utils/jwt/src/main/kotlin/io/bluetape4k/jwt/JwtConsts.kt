package io.bluetape4k.jwt

import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.SignatureAlgorithm
import java.time.Duration

/**
 * JWT 생성/파싱에 사용하는 공용 상수를 제공합니다.
 *
 * ## 동작/계약
 * - 헤더 키 상수는 JWT 표준 필드명(`typ`, `kid`, `alg`)을 그대로 사용합니다.
 * - 기본 키체인 저장소는 [InMemoryKeyChainRepository]를 lazy로 1회 생성해 재사용합니다.
 * - 기본 서명 알고리즘은 [Jwts.SIG.RS256]입니다.
 *
 * ```kotlin
 * val typ = JwtConsts.HEADER_TYPE_VALUE
 * val alg = JwtConsts.DefaultSignatureAlgorithm
 * // typ == "JWT"
 * // alg.id == "RS256"
 * ```
 */
object JwtConsts {

    /** JWT header의 type 키입니다. */
    const val HEADER_TYPE_KEY = "typ"

    /** JWT header의 type 값(`JWT`)입니다. */
    const val HEADER_TYPE_VALUE = "JWT"

    /** JWT header의 key id 키입니다. */
    const val HEADER_KEY_ID = "kid"

    /** JWT header의 signature algorithm 키입니다. */
    const val HEADER_ALGORITHM = "alg"

    /** 기본 키 로테이션 TTL(분)입니다. */
    val DEFAULT_KEY_ROTATION_TTL_MILLIS = Duration.ofDays(365).toMinutes()

    /** 기본 인메모리 키체인 저장소입니다. */
    val DefaultKeyChainRepository by lazy { InMemoryKeyChainRepository() }

    /** 기본 JWT 서명 알고리즘입니다. */
    val DefaultSignatureAlgorithm: SignatureAlgorithm = Jwts.SIG.RS256

    /** RSA 계열 서명 알고리즘 ID 목록입니다. */
    val RSA_ALGORITHM_IDS: Set<String> = setOf("RS256", "RS384", "RS512", "PS256", "PS384", "PS512")

    /**
     * 알고리즘 ID로 [SignatureAlgorithm]을 조회합니다.
     *
     * ```kotlin
     * val alg = JwtConsts.signatureAlgorithmForId("RS256")
     * // alg.id == "RS256"
     *
     * val alg512 = JwtConsts.signatureAlgorithmForId("RS512")
     * // alg512.id == "RS512"
     * ```
     *
     * @param algorithmId 알고리즘 식별자 (예: "RS256")
     * @return 해당하는 [SignatureAlgorithm] 인스턴스
     * @throws IllegalArgumentException 지원하지 않는 알고리즘인 경우
     */
    fun signatureAlgorithmForId(algorithmId: String): SignatureAlgorithm = when (algorithmId) {
        "RS256" -> Jwts.SIG.RS256
        "RS384" -> Jwts.SIG.RS384
        "RS512" -> Jwts.SIG.RS512
        "PS256" -> Jwts.SIG.PS256
        "PS384" -> Jwts.SIG.PS384
        "PS512" -> Jwts.SIG.PS512
        else -> throw IllegalArgumentException("Unsupported signature algorithm: $algorithmId")
    }
}
