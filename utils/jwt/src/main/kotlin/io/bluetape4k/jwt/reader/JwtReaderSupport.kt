package io.bluetape4k.jwt.reader

import io.bluetape4k.jwt.utils.epochSeconds
import io.bluetape4k.jwt.utils.epochSecondsOrNull
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.impl.DefaultClaims
import io.jsonwebtoken.impl.DefaultJws
import io.jsonwebtoken.impl.DefaultJwsHeader
import java.util.*

/**
 * [JwtReader]를 [JwtReaderDto]로 변환합니다.
 *
 * ## 동작/계약
 * - 헤더, 클레임, 서명 정보를 DTO에 복사합니다.
 */
fun JwtReader.toDto(): JwtReaderDto {
    return JwtReaderDto(
        mutableMapOf<String, Any?>().apply { putAll(jws.header) },
        mutableMapOf<String, Any?>().apply { putAll(jws.payload) },
        jws.digest
    )
}

/**
 * [JwtReaderDto]를 [JwtReader]로 변환합니다.
 *
 * ## 동작/계약
 * - 내부 jjwt 구현체(`DefaultJws`, `DefaultJwsHeader`, `DefaultClaims`)를 사용합니다.
 */
fun JwtReaderDto.toJwtReader(): JwtReader {
    val digestBytes = digest ?: byteArrayOf()
    val signatureStr = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes)
    return JwtReader(
        DefaultJws(
            DefaultJwsHeader(headers),
            DefaultClaims(claims),
            digestBytes,
            signatureStr
        )
    )
}

/**
 * JWT 정보가 만료되었는지 확인합니다.
 *
 * ## 동작/계약
 * - 만료되었다면 [ExpiredJwtException]을 발생시킵니다.
 *
 * @throws ExpiredJwtException JWT가 만료된 경우
 */
fun JwtReader.checkExpired() {
    if (isExpired) {
        val now = Date()
        val message = "JWT expired at $expiration. current time: $now " +
                "Elapsed time: ${now.epochSeconds - (expiration.epochSecondsOrNull ?: 0)} seconds"

        throw ExpiredJwtException(jws.header, jws.payload, message)
    }
}
