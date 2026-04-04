package io.bluetape4k.jwt.composer

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.support.requireNotBlank
import io.jsonwebtoken.Claims
import io.jsonwebtoken.io.CompressionAlgorithm
import java.util.*

@DslMarker
annotation class JwtComposerDslMarker

/**
 * DSL을 사용하여 JWT를 구성합니다.
 *
 * ```kotlin
 * val jwt = composeJwt(keyChain) {
 *     claim("service", "bluetape4k")
 *     expirationAfterMinutes = 60
 * }
 * ```
 *
 * @param keyChain 서명에 사용할 [KeyChain]
 * @param builder DSL 빌더 블록
 * @return 생성된 JWT 문자열
 */
inline fun composeJwt(
    keyChain: KeyChain,
    builder: JwtComposerDsl.() -> Unit,
): String {
    return JwtComposerDsl(keyChain).apply(builder).compose()
}

/**
 * JWT 구성을 위한 DSL 클래스입니다.
 *
 * ## 동작/계약
 * - 내부적으로 [JwtComposer]를 위임하여 JWT를 생성합니다.
 * - 예약 클레임(`exp`, `iat`, `nbf`)은 전용 프로퍼티/메서드로 설정합니다.
 *
 * @param keyChain 서명에 사용할 [KeyChain]
 */
@JwtComposerDslMarker
class JwtComposerDsl(keyChain: KeyChain) {

    private val composer = JwtComposer(keyChain)

    /** JWT ID (`jti`) 클레임입니다. */
    var id: String? = null
    /** JWT 발급자(`iss`) 클레임입니다. */
    var issuer: String? = null
    /** JWT 주제(`sub`) 클레임입니다. */
    var subject: String? = null
    /** JWT 대상(`aud`) 클레임입니다. */
    var audience: String? = null

    /** JWT 활성화 시각(`nbf`) 클레임입니다. */
    var notBefore: Date? = null
    /** JWT 활성화 시각을 초 단위로 지정합니다. [notBefore]와 함께 사용하지 마세요. */
    var notBeforeInSeconds: Long? = null

    /** JWT 만료 시각(`exp`) 클레임입니다. */
    var expiration: Date? = null
    /** 현재 시각 기준 만료까지의 시간(초)입니다. [expiration]과 함께 사용하지 마세요. */
    var expirationAfterSeconds: Long? = null
    /** 현재 시각 기준 만료까지의 시간(분)입니다. [expiration]과 함께 사용하지 마세요. */
    var expirationAfterMinutes: Long? = null

    /** JWT 발급 시각(`iat`) 클레임입니다. `null`이면 [compose] 시점의 현재 시각이 사용됩니다. */
    var issuedAt: Date? = null

    /**
     * 발급 시각을 현재 시각으로 설정합니다.
     *
     * ```kotlin
     * val jwt = composeJwt(KeyChain()) {
     *     issuedAtNow()
     *     expirationAfterMinutes = 60
     * }
     * // jwt.isNotBlank() == true
     * ```
     */
    fun issuedAtNow() = apply {
        composer.issuedAtNow()
    }

    /** JWT 페이로드 압축 알고리즘입니다. `null`이면 압축하지 않습니다. */
    var compressionAlgorithm: CompressionAlgorithm? = null

    /**
     * JWT Header 를 추가합니다.
     *
     * ```kotlin
     * val jwt = composeJwt(KeyChain()) {
     *     header("x-author", "debop")
     *     expirationAfterMinutes = 60
     * }
     * // provider.parse(jwt).header<String>("x-author") == "debop"
     * ```
     *
     * @param key  Header Key
     * @param value Header value
     */
    fun header(key: String, value: Any) = apply {
        key.requireNotBlank("key")
        if (key !in JwtComposer.RESERVED_HEADER_NAMES) {
            composer.header(key, value)
        }
    }

    /**
     * JWT Claim 을 추가합니다.
     *
     * ```kotlin
     * val jwt = composeJwt(KeyChain()) {
     *     claim("userId", "alice")
     *     claim("role", "admin")
     *     expirationAfterMinutes = 60
     * }
     * // provider.parse(jwt).claim<String>("userId") == "alice"
     * ```
     *
     * @param name claim name
     * @param value claim value
     * @param check validation
     */
    fun claim(name: String, value: Any, check: Boolean = true) = apply {
        name.requireNotBlank("name")
        if (check) {
            when (name) {
                Claims.EXPIRATION -> throw IllegalArgumentException("use expiration() instead of claim()")
                Claims.ISSUED_AT -> throw IllegalArgumentException("use setIssuedAt() instead of claim()")
                Claims.NOT_BEFORE -> throw IllegalArgumentException("use notBefore() instead of claim()")
            }
        }
        composer.claim(name, value)
    }

    /**
     * DSL 설정을 반영하여 JWT 문자열을 생성합니다.
     *
     * ```kotlin
     * val dsl = JwtComposerDsl(KeyChain()).apply {
     *     subject = "alice"
     *     issuer = "bluetape4k"
     *     expirationAfterMinutes = 60
     * }
     * val jwt = dsl.compose()
     * // jwt.count { it == '.' } == 2
     * ```
     *
     * @return 생성된 JWT 문자열
     */
    fun compose(): String {
        id?.run { composer.id(this) }
        issuer?.run { composer.issuer(this) }
        subject?.run { composer.subject(this) }
        audience?.run { composer.audience(this) }

        notBefore?.run { composer.notBefore(this) }
        notBeforeInSeconds?.run { composer.notBefore(this * 1000L) }

        expiration?.run { composer.expiration(this) }
        expirationAfterSeconds?.run { composer.expirationAfterSeconds(this) }
        expirationAfterMinutes?.run { composer.expirationAfterMinutes(this) }

        issuedAt?.run { composer.issuedAt(this) } ?: composer.issuedAtNow()

        // Compression
        compressionAlgorithm?.run { composer.setCompressionAlgorithm(this) }

        return composer.compose()
    }
}
