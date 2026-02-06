package io.bluetape4k.jwt.composer

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.support.requireNotBlank
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodec
import java.util.*

@DslMarker
annotation class JwtComposerDslMarker

inline fun composeJwt(
    keyChain: KeyChain,
    @BuilderInference builder: JwtComposerDsl.() -> Unit,
): String {
    return JwtComposerDsl(keyChain).apply(builder).compose()
}

@JwtComposerDslMarker
class JwtComposerDsl(keyChain: KeyChain) {

    private val composer = JwtComposer(keyChain)

    var id: String? = null
    var issuer: String? = null
    var subject: String? = null
    var audience: String? = null

    var notBefore: Date? = null
    var notBeforeInSeconds: Long? = null

    var expiration: Date? = null
    var expirationAfterSeconds: Long? = null
    var expirationAfterMinutes: Long? = null

    var issuedAt: Date? = null

    fun issuedAtNow() = apply {
        composer.issuedAtNow()
    }

    var compressionCodec: CompressionCodec? = null

    /**
     * JWT Header 를 추가합니다.
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
        compressionCodec?.run { composer.setCompressionCodec(this) }

        return composer.compose()
    }
}
