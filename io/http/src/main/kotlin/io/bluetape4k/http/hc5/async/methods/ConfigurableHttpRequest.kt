package io.bluetape4k.http.hc5.async.methods

import org.apache.hc.client5.http.async.methods.ConfigurableHttpRequest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.net.URIAuthority

/**
 * [ConfigurableHttpRequest]를 생성합니다.
 *
 * ```
 * val request = configurableHttpRequestOf("GET", "http://localhost:8080/api/v1")
 * ```
 *
 * @param method HTTP 메서드 이름
 * @param host [HttpHost] 정보
 * @param path 요청 경로
 * @return [ConfigurableHttpRequest] 인스턴스
 */
fun configurableHttpRequestOf(
    method: String,
    host: HttpHost,
    path: String,
): ConfigurableHttpRequest {
    return ConfigurableHttpRequest(method, host, path)
}

/**
 * [ConfigurableHttpRequest]를 생성합니다.
 *
 * ```
 * val request = configurableHttpRequestOf("GET", "http://localhost:8080/api/v1")
 * ```
 *
 * @param method HTTP 메서드 이름
 * @param scheme 스키마 정보
 * @param authority [URIAuthority] 정보
 * @param path 요청 경로
 * @return [ConfigurableHttpRequest] 인스턴스
 */
fun configurableHttpRequestOf(
    method: String,
    path: String,
    scheme: String? = null,
    authority: URIAuthority? = null,
): ConfigurableHttpRequest {
    return ConfigurableHttpRequest(method, scheme, authority, path)
}
