package io.bluetape4k.http.hc5.async.methods

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleResponseBuilder
import org.apache.hc.core5.http.ContentType

/**
 * [SimpleHttpResponse]를 생성합니다.
 *
 * ```
 * val response = simpleHttpResponse(200) {
 *    body("Hello, World!")
 *    contentType(ContentType.TEXT_PLAIN)
 *    header("X-Hello", "World")
 * }
 * ```
 *
 * @param status HTTP Status Code
 * @param initializer [SimpleResponseBuilder] 초기화 람다
 * @return [SimpleHttpResponse] 인스턴스
 */
inline fun simpleHttpResponse(
    status: Int,
    initializer: SimpleResponseBuilder.() -> Unit,
): SimpleHttpResponse {
    return SimpleResponseBuilder.create(status).apply(initializer).build()
}

/**
 * [SimpleHttpResponse]를 생성합니다.
 *
 * ```
 * val response = simpleHttpResponseOf(200, "Hello, World!")
 * ```
 *
 * @param status HTTP Status Code
 * @param content 응답 본문
 * @param contentType 응답 Content-Type
 * @param initializer [SimpleResponseBuilder] 초기화 람다
 * @return [SimpleHttpResponse] 인스턴스
 */
fun simpleHttpResponseOf(
    status: Int,
    content: String,
    contentType: ContentType = ContentType.TEXT_PLAIN,
    initializer: SimpleResponseBuilder.() -> Unit = {},
): SimpleHttpResponse =
    simpleHttpResponse(status) {
        setBody(content, contentType)
        initializer()
    }

/**
 * [SimpleHttpResponse]를 생성합니다.
 *
 * ```
 * val response = simpleHttpResponseOf(200, "Hello, World!".toUtf8Bytes())
 * ```
 *
 * @param status HTTP Status Code
 * @param content 응답 본문 (ByteArray)
 * @param contentType 응답 Content-Type
 * @param initializer [SimpleResponseBuilder] 초기화 람다
 * @return [SimpleHttpResponse] 인스턴스
 */
fun simpleHttpResponseOf(
    status: Int,
    content: ByteArray,
    contentType: ContentType = ContentType.TEXT_PLAIN,
    initializer: SimpleResponseBuilder.() -> Unit = {},
): SimpleHttpResponse =
    simpleHttpResponse(status) {
        setBody(content, contentType)
        initializer()
    }
