package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.message.BasicHttpResponse
import org.apache.hc.core5.http.support.BasicResponseBuilder

/**
 * [BasicHttpResponse] 를 빌드합니다.
 *
 * ```
 * val response = basicHttpResponse(200) {
 *     addHeader("Content-Type", "text/plain")
 *     setEntity("Hello, World!")
 * }
 * ```
 *
 * @param status [Int] HTTP Status Code
 * @param initializer [BasicResponseBuilder] 초기화 람다
 * @return [BasicHttpResponse] 인스턴스
 */
inline fun basicHttpResponse(
    status: Int,
    initializer: BasicResponseBuilder.() -> Unit,
): BasicHttpResponse {
    return BasicResponseBuilder.create(status).apply(initializer).build()
}

/**
 * [BasicHttpResponse] 를 빌드합니다.
 *
 * ```
 * val response = basicHttpResponse(response) {
 *     addHeader("Content-Type", "text/plain")
 *     setEntity("Hello, World!")
 * }
 * ```
 *
 * @param response [HttpResponse] 기본 응답 정보
 * @param initializer [BasicResponseBuilder] 초기화 람다
 * @return [BasicHttpResponse] 인스턴스
 */
fun basicHttpResponse(
    response: HttpResponse,
    initializer: BasicResponseBuilder.() -> Unit,
): BasicHttpResponse {
    return BasicResponseBuilder.copy(response).apply(initializer).build()
}
