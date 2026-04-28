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
 * - [tokenString]을 제공하면 캐시 복원 후 서명 재검증에 활용할 수 있습니다.
 *
 * ```kotlin
 * val provider = JwtProviderFactory.default()
 * val jwt = provider.compose { claim("userId", "alice"); expirationAfterMinutes = 60 }
 * val reader = provider.parse(jwt)
 * val dto = reader.toDto(jwt)
 * // dto.claims["userId"] == "alice"
 * // dto.headers["kid"] != null
 * // dto.tokenString == jwt
 * ```
 *
 * @param tokenString 원본 JWT 문자열. 캐시에서 복원 후 서명 재검증용으로 보관합니다.
 */
fun JwtReader.toDto(tokenString: String? = null): JwtReaderDto {
    return JwtReaderDto(
        mutableMapOf<String, Any?>().apply { putAll(jws.header) },
        mutableMapOf<String, Any?>().apply { putAll(jws.payload) },
        jws.digest,
        tokenString,
    )
}

/**
 * [JwtReaderDto]를 [JwtReader]로 변환합니다.
 *
 * ## 동작/계약
 * - 내부 jjwt 구현체(`DefaultJws`, `DefaultJwsHeader`, `DefaultClaims`)를 사용합니다.
 * - 서명 재검증을 수행하지 않습니다. 캐시 오염 시 위조된 claims를 수용할 수 있습니다.
 *
 * ## 보안 주의
 * - 보안이 중요한 환경에서는 이 메서드 대신 `provider.parse(dto.tokenString!!)` 를 사용하여
 *   서명을 재검증하세요.
 *
 * ```kotlin
 * val provider = JwtProviderFactory.default()
 * val jwt = provider.compose { claim("userId", "alice"); expirationAfterMinutes = 60 }
 * val reader = provider.parse(jwt)
 * val dto = reader.toDto(jwt)
 * val restored = dto.toJwtReader()
 * // restored.claim<String>("userId") == "alice"
 * // 서명 재검증이 필요하면: provider.parse(dto.tokenString!!)
 * ```
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
 * ```kotlin
 * val provider = JwtProviderFactory.default()
 * val jwt = provider.compose { expirationAfterSeconds = 3600 }
 * val reader = provider.parse(jwt)
 * reader.checkExpired() // 만료되지 않았으면 아무 일도 없음
 * // 만료된 경우: throws ExpiredJwtException
 * ```
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
