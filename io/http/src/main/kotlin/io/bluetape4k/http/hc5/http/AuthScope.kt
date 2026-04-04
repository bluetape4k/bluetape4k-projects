package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.core5.http.HttpHost

/**
 * [AuthScope] 를 생성합니다.
 *
 * ```kotlin
 * val authScope = authScopeOf("http", "localhost", 8080, realm = "admin", schemeName = "BASIC")
 * ```
 *
 * @param protocol 프로토콜
 * @param host 호스트
 * @param port 포트
 * @param realm 인증 영역
 * @param schemeName 스키마 이름
 * @return [AuthScope]
 */
fun authScopeOf(
    protocol: String,
    host: String,
    port: Int = -1,
    realm: String? = null,
    schemeName: String? = null,
): AuthScope = AuthScope(protocol, host, port, realm, schemeName)

/**
 * [AuthScope] 를 생성합니다.
 *
 * ```kotlin
 * val host = HttpHost("http", "localhost", 8080)
 * val authScope = authScopeOf(host, realm = "admin")
 * ```
 *
 * @param origin [HttpHost]
 * @param realm 인증 영역
 * @param schemeName 스키마 이름
 * @return [AuthScope]
 */
fun authScopeOf(
    origin: HttpHost,
    realm: String? = null,
    schemeName: String? = null,
): AuthScope = AuthScope(origin, realm, schemeName)

/**
 * URL 문자열로 [AuthScope] 를 생성합니다.
 *
 * ```kotlin
 * val authScope = authScopeOf("http://localhost:8080", realm = "admin")
 * ```
 *
 * @param url URL
 * @param realm 인증 영역
 * @param schemeName 스키마 이름
 * @return [AuthScope]
 */
fun authScopeOf(
    url: String,
    realm: String? = null,
    schemeName: String? = null,
): AuthScope = AuthScope(httpHostOf(url), realm, schemeName)

/**
 * 호스트와 포트로 [AuthScope]를 생성합니다.
 *
 * ```kotlin
 * val authScope = authScopeOf("example.com", 8080)
 * val credentialsProvider = credentialsProviderOf(
 *     authScope,
 *     "user",
 *     "password".toCharArray()
 * )
 * ```
 *
 * @param host 호스트 이름
 * @param port 포트 번호
 * @return [AuthScope]
 */
fun authScopeOf(
    host: String,
    port: Int,
): AuthScope = AuthScope(host, port)
