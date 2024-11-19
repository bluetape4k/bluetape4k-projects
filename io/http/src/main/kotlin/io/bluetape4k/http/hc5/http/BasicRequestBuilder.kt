package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.BasicHttpRequest
import org.apache.hc.core5.http.support.BasicRequestBuilder

/**
 * [BasicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = basicHttpRequest("GET") {
 *     setHttpHost(HttpHost("localhost", 8080))
 *     setPath("/api/v1")
 * }
 * ```
 *
 * @param method HTTP Method
 * @param initializer [BasicRequestBuilder] 초기화 람다
 * @return [BasicHttpRequest]
 */
inline fun basicHttpRequest(
    method: String,
    initializer: BasicRequestBuilder.() -> Unit,
): BasicHttpRequest {
    return BasicRequestBuilder.create(method).apply(initializer).build()
}

/**
 * [BasicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = basicHttpRequest(Method.GET) {
 *     setHttpHost(HttpHost("localhost", 8080))
 *     setPath("/api/v1")
 * }
 * ```
 *
 * @param method [Method]
 * @param initializer [BasicRequestBuilder] 초기화 람다
 * @return [BasicHttpRequest]
 */
inline fun basicHttpRequest(
    method: Method,
    initializer: BasicRequestBuilder.() -> Unit,
): BasicHttpRequest = basicHttpRequest(method.name, initializer)

/**
 * [BasicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = basicHttpRequestOf("GET", HttpHost("localhost", 8080), "/api/v1") {
 *      setHeader("Authorization", "Bearer token")
 *      setHeader("Content-Type", "application/json")
 *      setHeader("Accept", "application/json")
 * }
 * ```
 *
 * @param method HTTP Method
 * @param host [HttpHost]
 * @param path 요청 경로
 * @param headers [Header] 리스트
 * @param initializer [BasicRequestBuilder] 초기화 람다
 * @return [BasicHttpRequest]
 */
fun basicHttpRequestOf(
    method: String,
    host: HttpHost,
    path: String,
    headers: Iterable<Header>? = null,
    initializer: BasicRequestBuilder.() -> Unit = {},
): BasicHttpRequest = basicHttpRequest(method) {
    setHttpHost(host)
    setPath(path)
    headers?.run { setHeaders(headers.iterator()) }

    initializer()
}

/**
 * [BasicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = basicHttpRequestOf(Method.GET, HttpHost("localhost", 8080), "/api/v1") {
 *      setHeader("Authorization", "Bearer token")
 *      setHeader("Content-Type", "application/json")
 *      setHeader("Accept", "application/json")
 * }
 * ```
 *
 * @param method [Method]
 * @param host [HttpHost]
 * @param path 요청 경로
 * @param headers [Header] 리스트
 * @param initializer [BasicRequestBuilder] 초기화 람다
 * @return [BasicHttpRequest]
 */
fun basicHttpRequestOf(
    method: Method,
    host: HttpHost,
    path: String,
    headers: Iterable<Header>? = null,
    initializer: BasicRequestBuilder.() -> Unit = {},
): BasicHttpRequest = basicHttpRequest(method) {
    setHttpHost(host)
    setPath(path)
    headers?.run { setHeaders(headers.iterator()) }

    initializer()
}
