package io.bluetape4k.http.hc5.fluent

import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.Method
import java.net.URI

/**
 * Fluent API의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestOf(Method.GET, URI.create("https://example.com"))
 * ```
 *
 * @param method 요청 메소드
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestOf(method: Method, uri: URI): Request = Request.create(method, uri)

/**
 * Fluent API의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestOf(Method.GET, "https://example.com")
 * ```
 *
 * @param methodName 요청 메소드
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestOf(methodName: String, uri: URI): Request = Request.create(methodName, uri)

/**
 * Fluent API의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestOf("GET", "https://example.com")
 * ```
 *
 * @param methodName 요청 메소드 [String]
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestOf(methodName: String, uri: String): Request = Request.create(methodName, uri)

/**
 * Fluent API의 GET 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestGet(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestGet(uri: URI): Request = Request.get(uri)

/**
 * Fluent API의 GET 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestGet("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestGet(uri: String): Request = Request.get(uri)

/**
 * Fluent API의 HEAD 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestHead(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestHead(uri: URI): Request = Request.head(uri)

/**
 * Fluent API의 HEAD 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestHead("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestHead(uri: String): Request = Request.head(uri)

/**
 * Fluent API의 POST 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPost(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestPost(uri: URI): Request = Request.post(uri)

/**
 * Fluent API의 POST 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPost("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestPost(uri: String): Request = Request.post(uri)

/**
 * Fluent API의 PATCH 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPatch(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestPatch(uri: URI): Request = Request.patch(uri)

/**
 * Fluent API의 PATCH 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPatch("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestPatch(uri: String): Request = Request.patch(uri)

/**
 * Fluent API의 PUT 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPut(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestPut(uri: URI): Request = Request.put(uri)

/**
 * Fluent API의 PUT 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestPut("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestPut(uri: String): Request = Request.put(uri)

/**
 * Fluent API의 TRACE 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestTrace(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestTrace(uri: URI): Request = Request.trace(uri)

/**
 * Fluent API의 TRACE 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestTrace("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestTrace(uri: String): Request = Request.trace(uri)

/**
 * Fluent API의 DELETE 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestDelete(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestDelete(uri: URI): Request = Request.delete(uri)

/**
 * Fluent API의 DELETE 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestDelete("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestDelete(uri: String): Request = Request.delete(uri)

/**
 * Fluent API의 OPTIONS 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestOptions(URI.create("https://example.com"))
 * ```
 *
 * @param uri 요청 주소 [URI]
 * @return 요청 정보 [Request]
 */
fun requestOptions(uri: URI): Request = Request.options(uri)

/**
 * Fluent API의 OPTIONS 방식의 [Request]를 생성합니다.
 *
 * ```
 * val request = requestOptions("https://example.com")
 * ```
 *
 * @param uri 요청 주소 [String]
 * @return 요청 정보 [Request]
 */
fun requestOptions(uri: String): Request = Request.options(uri)
