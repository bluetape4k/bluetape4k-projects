package io.bluetape4k.http.hc5.async.methods

import org.apache.hc.client5.http.async.methods.SimpleBody
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method

/**
 * [SimpleHttpRequest]를 생성합니다.
 *
 * ```
 * val request = simpleHttpRequest("GET") {
 *     setHttpHost(HttpHost("localhost", 8080))
 *     setPath("/api/v1")
 * }
 * ```
 *
 * @param method HTTP Method
 * @param initializer [SimpleRequestBuilder] 초기화 람다
 * @return [SimpleHttpRequest] 인스턴스
 */
inline fun simpleHttpRequest(
    method: String,
    initializer: SimpleRequestBuilder.() -> Unit,
): SimpleHttpRequest {
    return SimpleRequestBuilder.create(method).apply(initializer).build()
}

/**
 * [SimpleHttpRequest]를 생성합니다.
 *
 * ```
 * val request = simpleHttpRequest(Method.GET) {
 *     setHttpHost(HttpHost("localhost", 8080))
 *     setPath("/api/v1")
 * }
 * ```
 *
 * @param method [Method] HTTP Method
 * @param initializer [SimpleRequestBuilder] 초기화 람다
 * @return [SimpleHttpRequest] 인스턴스
 */
inline fun simpleHttpRequest(
    method: Method,
    initializer: SimpleRequestBuilder.() -> Unit,
): SimpleHttpRequest {
    return SimpleRequestBuilder.create(method).apply(initializer).build()
}

/**
 * [SimpleHttpRequest]를 생성합니다.
 *
 * ```
 * val request = simpleHttpRequestOf("GET", HttpHost("localhost", 8080), "/api/v1")
 * ```
 *
 * @param method HTTP Method
 * @param host [HttpHost] 정보
 * @param path 요청 경로
 * @param body [SimpleBody] 정보
 * @param headers [Header] 정보
 * @param initializer [SimpleRequestBuilder] 초기화 람다
 * @return [SimpleHttpRequest] 인스턴스
 */
fun simpleHttpRequestOf(
    method: String,
    host: HttpHost,
    path: String,
    body: SimpleBody? = null,
    headers: Iterable<Header>? = null,
    initializer: SimpleRequestBuilder.() -> Unit = {},
): SimpleHttpRequest = simpleHttpRequest(method) {
    setHttpHost(host)
    setPath(path)
    body?.run { setBody(body) }
    headers?.run { setHeaders(headers.iterator()) }

    initializer()
}

/**
 * [SimpleHttpRequest]를 생성합니다.
 *
 * ```
 * val request = simpleHttpRequestOf(Method.GET, HttpHost("localhost", 8080), "/api/v1")
 * ```
 *
 * @param method [Method] HTTP Method
 * @param host [HttpHost] 정보
 * @param path 요청 경로
 * @param body [SimpleBody] 정보
 * @param headers [Header] 정보
 * @param initializer [SimpleRequestBuilder] 초기화 람다
 * @return [SimpleHttpRequest] 인스턴스
 */
fun simpleHttpRequestOf(
    method: Method,
    host: HttpHost,
    path: String,
    body: SimpleBody? = null,
    headers: Iterable<Header>? = null,
    initializer: SimpleRequestBuilder.() -> Unit = {},
): SimpleHttpRequest = simpleHttpRequest(method) {
    setHttpHost(host)
    setPath(path)
    body?.run { setBody(body) }
    headers?.run { setHeaders(headers.iterator()) }
    initializer()
}

/**
 * [SimpleHttpRequest]를 이용하여 [SimpleRequestProducer]를 생성합니다.
 *
 * ```
 * val producer = simpleHttpRequest("GET") {
 *    setHttpHost(HttpHost("localhost", 8080))
 *    setPath("/api/v1")
 *    setBody(StringBody("Hello, World!"))
 * }.toProducer()
 * ```
 *
 * @receiver [SimpleHttpRequest] 인스턴스
 * @return [SimpleRequestProducer] 인스턴스
 */
fun SimpleHttpRequest.toProducer(): SimpleRequestProducer =
    simpleRequestProducerOf(this)
