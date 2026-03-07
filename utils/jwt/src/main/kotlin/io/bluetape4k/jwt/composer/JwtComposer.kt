package io.bluetape4k.jwt.composer

import io.bluetape4k.jwt.JwtConsts.HEADER_ALGORITHM
import io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID
import io.bluetape4k.jwt.JwtConsts.HEADER_TYPE_KEY
import io.bluetape4k.jwt.JwtConsts.HEADER_TYPE_VALUE
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.jwt.utils.millisToSeconds
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireNotBlank
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.CompressionAlgorithm
import java.util.*

/**
 * JWT 를 구성합니다.
 *
 * ## 동작/계약
 * - 예약 헤더(`kid`, `alg`)는 [header]로 덮어쓸 수 없습니다.
 * - 예약 클레임(`exp`, `iat`, `nbf`)은 전용 메서드 사용을 강제합니다.
 * - [compose] 시 `iat`가 없으면 현재 시각으로 자동 설정됩니다.
 *
 * ```kotlin
 * val jwt = JwtComposer(KeyChain())
 *     .header("x-author", "debop")
 *     .claim("service", "bluetape4k")
 *     .expirationAfterMinutes(60)
 *     .compose()
 * // jwt.isNotBlank() == true
 * ```
 */
class JwtComposer(
    private val keyChain: KeyChain,
    internal val headers: MutableMap<String, Any> = mutableMapOf(),
    internal val claims: MutableMap<String, Any> = mutableMapOf(),
) {

    companion object: KLogging() {
        /** `JwtComposer`에서 사용자 설정이 제한되는 헤더 목록입니다. */
        val RESERVED_HEADER_NAMES: List<String> = listOf(HEADER_KEY_ID, HEADER_ALGORITHM)
    }

    private var compressionAlgorithm: CompressionAlgorithm? = null

    /**
     * JWT 압축 알고리즘을 설정합니다.
     *
     * ## 동작/계약
     * - 설정된 알고리즘은 [compose] 시 `zip` 헤더와 함께 적용됩니다.
     * - 알고리즘이 없으면 비압축 JWT를 생성합니다.
     *
     * @param algorithm JWT payload 압축에 사용할 [CompressionAlgorithm]
     */
    fun setCompressionAlgorithm(algorithm: CompressionAlgorithm) {
        compressionAlgorithm = algorithm
    }

    /**
     * JWT Header 를 추가합니다.
     *
     * ## 동작/계약
     * - [key]는 공백이 아니어야 하며 위반 시 예외가 발생합니다.
     * - 예약 헤더 키는 무시됩니다.
     *
     * @param key  Header Key
     * @param value Header value
     */
    fun header(key: String, value: Any) = apply {
        key.requireNotBlank("key")

        if (key !in JwtComposer.RESERVED_HEADER_NAMES) {
            headers[key] = value
        }
    }

    /**
     * JWT Claim 을 추가합니다.
     *
     * ## 동작/계약
     * - [name]은 공백이 아니어야 합니다.
     * - `check=true`일 때 예약 클레임(`exp`,`iat`,`nbf`)은 예외를 던집니다.
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
        claims[name] = value
    }

    /** JWT ID(`jti`) 클레임을 설정합니다. */
    fun id(jti: String) = claim(Claims.ID, jti)
    /** 발급자(`iss`) 클레임을 설정합니다. */
    fun issuer(iss: String) = claim(Claims.ISSUER, iss)
    /** 주체(`sub`) 클레임을 설정합니다. */
    fun subject(sub: String) = claim(Claims.SUBJECT, sub)
    /** 수신자(`aud`) 클레임을 설정합니다. */
    fun audience(aud: String) = claim(Claims.AUDIENCE, aud)

    /** `nbf` 클레임을 [Date]로 설정합니다. */
    fun notBefore(nbfDate: Date) =
        claim(Claims.NOT_BEFORE, nbfDate.epochSeconds, false)

    /** `nbf` 클레임을 밀리초 타임스탬프로 설정합니다. */
    fun notBefore(nbfTimestamp: Long) =
        claim(Claims.NOT_BEFORE, nbfTimestamp.millisToSeconds(), false)

    /** 만료(`exp`) 클레임을 [Date]로 설정합니다. */
    fun expiration(exp: Date) =
        claim(Claims.EXPIRATION, exp.epochSeconds, false)

    /** 현재 시각 기준 [seconds]초 뒤 만료되도록 설정합니다. */
    fun expirationAfterSeconds(seconds: Long) =
        claim(Claims.EXPIRATION, Date().epochSeconds + seconds, false)

    /** 현재 시각 기준 [minutes]분 뒤 만료되도록 설정합니다. */
    fun expirationAfterMinutes(minutes: Long) =
        expirationAfterSeconds(minutes * 60)

    /** 현재 시각 기준 [days]일 뒤 만료되도록 설정합니다. */
    fun expirationAfterDays(days: Long) =
        expirationAfterSeconds(days * 24 * 60 * 60)

    /** 발급 시각(`iat`) 클레임을 설정합니다. */
    fun issuedAt(iat: Date) = claim(Claims.ISSUED_AT, iat.epochSeconds, false)
    /** 발급 시각(`iat`)을 현재 시각으로 설정합니다. */
    fun issuedAtNow() = issuedAt(Date())

    /**
     * 현재 설정으로 JWT 문자열을 생성합니다.
     *
     * ## 동작/계약
     * - 헤더에 `kid`, `typ=JWT`를 강제로 설정하고 private key로 서명합니다.
     * - claim/headers를 모두 반영한 URL-safe compact JWT를 반환합니다.
     *
     * ```kotlin
     * val jwt = composer.compose()
     * // jwt.count { it == '.' } == 2
     * ```
     */
    fun compose(): String {
        log.debug { "Compose JWT. keyChain id=${keyChain.id}, algorithm=${keyChain.algorithm.id}" }

        return jwt {
            header().add(HEADER_KEY_ID, keyChain.id)
            header().add(HEADER_TYPE_KEY, HEADER_TYPE_VALUE)
            signWith(keyChain.keyPair.private, keyChain.algorithm)

            headers.forEach { (key, value) ->
                if (key !in JwtComposer.RESERVED_HEADER_NAMES) {
                    log.trace { "set jwt header. key=$key, value=$value" }
                    header().add(key, value)
                }
            }
            claims.forEach { (name, value) ->
                log.trace { "set claim. name=$name, value=$value" }
                claim(name, value)
            }
            if (claims[Claims.ISSUED_AT] == null) issuedAt(Date())

            compressionAlgorithm?.let { compressWith(it) }
        }
    }

    private inline fun jwt(setup: JwtBuilder.() -> Unit): String {
        return Jwts.builder().apply(setup).compact()
    }
}
