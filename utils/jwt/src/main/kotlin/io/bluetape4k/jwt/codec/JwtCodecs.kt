package io.bluetape4k.jwt.codec

import io.bluetape4k.jwt.codec.JwtCodecs.Deflate
import io.bluetape4k.jwt.codec.JwtCodecs.Gzip
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.CompressionAlgorithm

/**
 * JWT 압축 알고리즘 팩토리 객체입니다.
 *
 * jjwt 0.13.0 내장 압축 알고리즘만 지원합니다:
 * - [Deflate]: DEFLATE 압축 (`Jwts.ZIP.DEF`)
 * - [Gzip]: GZIP 압축 (`Jwts.ZIP.GZIP`)
 *
 * ```kotlin
 * val composer = JwtComposer(KeyChain())
 * composer.setCompressionAlgorithm(JwtCodecs.Gzip)
 * val jwt = composer.compose()
 * // jwt.isNotBlank() == true
 * ```
 */
object JwtCodecs {

    /**
     * DEFLATE 압축 알고리즘
     *
     * ```kotlin
     * val composer = JwtComposer(KeyChain())
     * composer.setCompressionAlgorithm(JwtCodecs.Deflate)
     * val jwt = composer.compose()
     * // jwt.isNotBlank() == true
     * ```
     */
    val Deflate: CompressionAlgorithm = Jwts.ZIP.DEF

    /**
     * GZIP 압축 알고리즘
     *
     * ```kotlin
     * val composer = JwtComposer(KeyChain())
     * composer.setCompressionAlgorithm(JwtCodecs.Gzip)
     * val jwt = composer.compose()
     * // jwt.isNotBlank() == true
     * ```
     */
    val Gzip: CompressionAlgorithm = Jwts.ZIP.GZIP
}
