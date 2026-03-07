package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.reader.JwtReader
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.security.SignatureAlgorithm

/**
 * JWT 키 관리와 생성/파싱을 제공하는 공급자 계약입니다.
 *
 * ## 동작/계약
 * - [parse]는 파싱 실패 시 [JwtException]을 던집니다.
 * - [tryParse]는 실패를 `null`로 반환합니다.
 * - [composer]/[compose]는 현재 키체인(또는 지정 키체인)으로 서명 JWT를 생성합니다.
 *
 * ```kotlin
 * val jwt = provider.compose { claim("claim1", "value") }
 * val reader = provider.parse(jwt)
 * // reader.claim<String>("claim1") == "value"
 * ```
 */
interface JwtProvider {

    companion object: KLogging()

    /** JWT 서명 알고리즘입니다. */
    val signatureAlgorithm: SignatureAlgorithm

    /**
     * [signatureAlgorithm]으로 기본 키체인을 생성합니다.
     *
     * ## 동작/계약
     * - 기본 구현은 [KeyChain] 생성자를 그대로 위임합니다.
     */
    fun createKeyChain(): KeyChain = KeyChain(signatureAlgorithm)

    /** 현재 서명에 사용하는 키체인을 반환합니다. */
    fun currentKeyChain(): KeyChain

    /** 저장소 정책에 따라 키체인 회전을 수행합니다. */
    fun rotate(): Boolean

    /** 현재 키체인을 강제로 교체합니다. */
    fun forcedRotate(): Boolean

    /** `kid`로 키체인을 조회합니다. */
    fun findKeyChain(kid: String): KeyChain?

    /** JWT 조합기 객체를 생성합니다. */
    fun composer(keyChain: KeyChain? = null): JwtComposer

    /** DSL로 JWT를 구성해 문자열을 반환합니다. */
    fun compose(keyChain: KeyChain? = null, @BuilderInference builder: JwtComposerDsl.() -> Unit): String

    /**
     * JWT 문자열을 파싱해 [JwtReader]를 반환합니다.
     *
     * ## 동작/계약
     * - 실패 시 [JwtException]을 던집니다.
     *
     * ```kotlin
     * val reader = provider.parse(jwt)
     * // reader.kid != null
     * ```
     */
    fun parse(jwtString: String): JwtReader {
        return tryParse(jwtString) ?: throw JwtException("Invalid jwt string: $jwtString")
    }

    /**
     * JWT 문자열 파싱을 시도합니다.
     *
     * ## 동작/계약
     * - 성공 시 [JwtReader], 실패 시 `null`을 반환합니다.
     *
     * ```kotlin
     * val reader = provider.tryParse("broken-jwt")
     * // reader == null
     * ```
     */
    fun tryParse(jwtString: String): JwtReader? = runCatching {
        val jws = this.currentJwtParser().parseSignedClaims(jwtString)
        JwtReader(jws)
    }.getOrNull()
}
