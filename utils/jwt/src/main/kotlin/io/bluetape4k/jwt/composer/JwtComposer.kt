package io.bluetape4k.jwt.composer

import io.bluetape4k.jwt.JwtConsts.HEADER_ALGORITHM
import io.bluetape4k.jwt.JwtConsts.HEADER_KEY_ID
import io.bluetape4k.jwt.JwtConsts.HEADER_TYPE_KEY
import io.bluetape4k.jwt.JwtConsts.HEADER_TYPE_VALUE
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.jwt.utils.millisToSeconds
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireNotBlank
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodec
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import java.util.*

/**
 * JWT 를 구성합니다.
 *
 * @property keyChain JWT 토큰 생성을 위한 암호 정보
 * @property headers Header 정보
 * @property claims Claim 정보
 */
class JwtComposer(
    private val keyChain: KeyChain,
    internal val headers: MutableMap<String, Any> = mutableMapOf(),
    internal val claims: MutableMap<String, Any> = mutableMapOf(),
) {

    companion object: KLogging() {
        val RESERVED_HEADER_NAMES: List<String> = listOf(HEADER_KEY_ID, HEADER_ALGORITHM)
    }

    private var compressionCodec: CompressionCodec? = null

    fun setCompressionCodec(codec: CompressionCodec) {
        compressionCodec = codec
    }

    /**
     * JWT Header 를 추가합니다.
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

    fun id(jti: String) = claim(Claims.ID, jti)
    fun issuer(iss: String) = claim(Claims.ISSUER, iss)
    fun subject(sub: String) = claim(Claims.SUBJECT, sub)
    fun audience(aud: String) = claim(Claims.AUDIENCE, aud)

    fun notBefore(nbfDate: Date) =
        claim(Claims.NOT_BEFORE, nbfDate.epochSeconds, false)

    fun notBefore(nbfTimestamp: Long) =
        claim(Claims.NOT_BEFORE, nbfTimestamp.millisToSeconds(), false)

    fun expiration(exp: Date) =
        claim(Claims.EXPIRATION, exp.epochSeconds, false)

    fun expirationAfterSeconds(seconds: Long) =
        claim(Claims.EXPIRATION, Date().epochSeconds + seconds, false)

    fun expirationAfterMinutes(minutes: Long) =
        expirationAfterSeconds(minutes * 60)

    fun expirationAfterDays(days: Long) =
        expirationAfterSeconds(days * 24 * 60 * 60)

    fun issuedAt(iat: Date) = claim(Claims.ISSUED_AT, iat.epochSeconds, false)
    fun issuedAtNow() = issuedAt(Date())

    /**
     * Actually builds the JWT and serializes it to a compact, URL-safe string according to the
     * [JWT Compact Serialization](https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-25#section-7)
     * rules.
     *
     * @return A compact URL-safe JWT string.
     */
    fun compose(): String {
        log.trace { "Compose JWT. keyChain id=${keyChain.id}, algorithm=${keyChain.algorithm.name}" }

        return jwt {
            setHeaderParam(HEADER_KEY_ID, keyChain.id)
            setHeaderParam(HEADER_TYPE_KEY, HEADER_TYPE_VALUE)
            signWith(keyChain.keyPair.private, keyChain.algorithm)

            headers.forEach { (key, value) ->
                if (key !in JwtComposer.RESERVED_HEADER_NAMES) {
                    log.trace { "set jwt header. key=$key, value=$value" }
                    setHeaderParam(key, value)
                }
            }
            claims.forEach { (name, value) ->
                log.trace { "set claim. name=$name, value=$value" }
                claim(name, value)
            }
            if (claims[Claims.ISSUED_AT] == null) issuedAtNow()

            compressionCodec?.let { compressWith(it) }
        }
    }

    private inline fun jwt(setup: JwtBuilder.() -> Unit): String {
        return Jwts.builder().apply(setup).compact()
    }
}
