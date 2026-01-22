package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.reader.JwtReader
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.SignatureAlgorithm

interface JwtProvider {

    companion object: KLogging()

    val signatureAlgorithm: SignatureAlgorithm

    fun createKeyChain(): KeyChain = KeyChain(signatureAlgorithm)

    fun currentKeyChain(): KeyChain

    fun rotate(): Boolean

    fun forcedRotate(): Boolean

    fun findKeyChain(kid: String): KeyChain?

    fun composer(keyChain: KeyChain? = null): JwtComposer

    fun compose(keyChain: KeyChain? = null, initializer: JwtComposerDsl.() -> Unit): String

    fun parse(jwtString: String): JwtReader {
        return tryParse(jwtString) ?: throw JwtException("Invalid jwt string: $jwtString")
    }

    fun tryParse(jwtString: String): JwtReader? = runCatching {
        val jws: Jws<Claims> = this.currentJwtParser().parseClaimsJws(jwtString)
        JwtReader(jws)
    }.getOrNull()
}
