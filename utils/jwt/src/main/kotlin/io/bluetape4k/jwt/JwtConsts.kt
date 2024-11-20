package io.bluetape4k.jwt

import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository
import io.jsonwebtoken.SignatureAlgorithm
import java.time.Duration

object JwtConsts {

    const val HEADER_TYPE_KEY = "typ"
    const val HEADER_TYPE_VALUE = "JWT"
    const val HEADER_KEY_ID = "kid"
    const val HEADER_ALGORITHM = "alg"

    val DEFAULT_KEY_ROTATION_TTL_MILLIS = Duration.ofDays(365).toMinutes()

    val DefaultKeyChainRepository by lazy { InMemoryKeyChainRepository() }
    val DefaultSignatureAlgorithm = SignatureAlgorithm.RS256
}
