package io.bluetape4k.jwt.codec

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.CompressionAlgorithm

/**
 * JWT 압축 알고리즘 팩토리 객체입니다.
 *
 * jjwt 0.13.0 내장 압축 알고리즘만 지원합니다:
 * - [Deflate]: DEFLATE 압축 (`Jwts.ZIP.DEF`)
 * - [Gzip]: GZIP 압축 (`Jwts.ZIP.GZIP`)
 */
object JwtCodecs {

    /** DEFLATE 압축 알고리즘 */
    val Deflate: CompressionAlgorithm = Jwts.ZIP.DEF

    /** GZIP 압축 알고리즘 */
    val Gzip: CompressionAlgorithm = Jwts.ZIP.GZIP
}
