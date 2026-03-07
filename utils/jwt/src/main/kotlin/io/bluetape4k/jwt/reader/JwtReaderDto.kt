package io.bluetape4k.jwt.reader

import java.io.Serializable

/**
 * [JwtReader]의 직렬화/캐싱용 DTO입니다.
 *
 * ## 동작/계약
 * - [headers]는 JWT 헤더의 키-값 맵입니다.
 * - [claims]는 JWT 클레임의 키-값 맵입니다.
 * - [digest]는 JWT 서명의 바이트 배열입니다 (jjwt 0.12.x+).
 *
 * @property headers JWT 헤더 맵
 * @property claims JWT 클레임 맵
 * @property digest JWT 서명 바이트 배열
 */
data class JwtReaderDto(
    val headers: Map<String, Any?> = mutableMapOf(),
    val claims: Map<String, Any?> = mutableMapOf(),
    val digest: ByteArray? = null,
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JwtReaderDto) return false
        return headers == other.headers &&
                claims == other.claims &&
                digest.contentEquals(other.digest)
    }

    override fun hashCode(): Int {
        var result = headers.hashCode()
        result = 31 * result + claims.hashCode()
        result = 31 * result + (digest?.contentHashCode() ?: 0)
        return result
    }
}
