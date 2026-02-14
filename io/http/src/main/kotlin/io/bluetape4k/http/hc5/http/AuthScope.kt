package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.core5.http.HttpHost

/**
 * [AuthScope] 를 생성합니다.
 *
 * ```
 * val authScope = authScopeOf("http://localhost:8080")
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
 * ```
 * val authScope = authScopeOf("http://localhost:8080")
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
 * [AuthScope] 를 생성합니다.
 *
 * ```
 * val authScope = authScopeOf("http://localhost:8080")
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
 * HTTP 처리에서 `authScopeOf` 함수를 제공합니다.
 */
fun authScopeOf(
    host: String,
    port: Int,
): AuthScope = AuthScope(host, port)
