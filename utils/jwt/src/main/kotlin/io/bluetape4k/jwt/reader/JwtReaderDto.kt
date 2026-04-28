package io.bluetape4k.jwt.reader

import java.io.Serializable

/**
 * [JwtReader]의 직렬화/캐싱용 DTO입니다.
 *
 * ## 동작/계약
 * - [headers]는 JWT 헤더의 키-값 맵입니다.
 * - [claims]는 JWT 클레임의 키-값 맵입니다.
 * - [digest]는 JWT 서명의 바이트 배열입니다 (jjwt 0.12.x+).
 * - [tokenString]은 원본 JWT 문자열입니다. 캐시에서 복원 시 서명 재검증에 사용합니다.
 *
 * ## 보안 주의
 * - 캐시에서 복원된 DTO를 [toJwtReader]로 변환하면 서명 재검증이 수행되지 않습니다.
 * - 보안이 중요한 환경에서는 [tokenString]을 이용해 `provider.parse(dto.tokenString!!)` 로
 *   재파싱하여 서명을 재검증하세요.
 *
 * ```kotlin
 * val provider = JwtProviderFactory.default()
 * val jwt = provider.compose { claim("userId", "alice"); expirationAfterMinutes = 60 }
 * val reader = provider.parse(jwt)
 * val dto = reader.toDto(jwt)
 * // dto.claims["userId"] == "alice"
 * // dto.headers.containsKey("kid") == true
 * // dto.tokenString == jwt  (서명 재검증에 활용 가능)
 * ```
 *
 * @property headers JWT 헤더 맵
 * @property claims JWT 클레임 맵
 * @property digest JWT 서명 바이트 배열
 * @property tokenString 원본 JWT 문자열 (캐시 복원 후 서명 재검증 용도)
 */
data class JwtReaderDto(
    val headers: Map<String, Any?> = mutableMapOf(),
    val claims: Map<String, Any?> = mutableMapOf(),
    val digest: ByteArray? = null,
    val tokenString: String? = null,
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JwtReaderDto) return false
        return headers == other.headers &&
                claims == other.claims &&
                digest.contentEquals(other.digest) &&
                tokenString == other.tokenString
    }

    override fun hashCode(): Int {
        var result = headers.hashCode()
        result = 31 * result + claims.hashCode()
        result = 31 * result + (digest?.contentHashCode() ?: 0)
        result = 31 * result + (tokenString?.hashCode() ?: 0)
        return result
    }
}
