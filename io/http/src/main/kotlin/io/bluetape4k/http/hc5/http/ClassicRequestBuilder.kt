package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder

/**
 * [ClassicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = classicRequest("GET") {
 *     setUri("https://example.com")
 * }
 * ```
 *
 * @param methodName HTTP Method Name
 * @param builder [ClassicRequestBuilder] 초기화 람다
 * @return [ClassicHttpRequest] 인스턴스
 */
inline fun classicRequest(
    methodName: String,
    @BuilderInference builder: ClassicRequestBuilder.() -> Unit,
): ClassicHttpRequest =
    ClassicRequestBuilder.create(methodName).apply(builder).build()

/**
 * [ClassicHttpRequest] 를 생성합니다.
 *
 * ```
 * val request = classicRequest(Method.GET) {
 *     setUri("https://example.com")
 * }
 * ```
 *
 * @param method [Method] HTTP Method
 * @param builder [ClassicRequestBuilder] 초기화 람다
 * @return [ClassicHttpRequest] 인스턴스
 */
inline fun classicRequest(
    method: Method,
    @BuilderInference builder: ClassicRequestBuilder.() -> Unit,
): ClassicHttpRequest =
    ClassicRequestBuilder.create(method.name).apply(builder).build()
