package io.bluetape4k.jwt.reader

import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.jwt.utils.epochSecondsOrNull
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.impl.DefaultClaims
import io.jsonwebtoken.impl.DefaultJws
import io.jsonwebtoken.impl.DefaultJwsHeader
import java.util.*

fun JwtReader.toDto(): JwtReaderDto {
    return JwtReaderDto(
        mutableMapOf<String, Any?>().apply { putAll(jws.header) },
        mutableMapOf<String, Any?>().apply { putAll(jws.body) },
        jws.signature
    )
}

fun JwtReaderDto.toJwtReader(): JwtReader {
    return JwtReader(
        DefaultJws(
            DefaultJwsHeader(headers),
            DefaultClaims(claims),
            signature
        )
    )
}

/**
 * JWT 정보가 만료되었는지 확인한다. 만료되었다면 [ExpiredJwtException]을 발생시킨다.
 */
fun JwtReader.checkExpired() {
    if (isExpired) {
        val now = Date()
        val message = "JWT expired at $expiration. current time: $now " +
                "Elapsed time: ${now.epochSeconds - (expiration.epochSecondsOrNull ?: 0)} seconds"

        throw ExpiredJwtException(jws.header, jws.body, message)
    }
}
