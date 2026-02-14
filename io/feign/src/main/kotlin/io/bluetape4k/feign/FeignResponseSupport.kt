package io.bluetape4k.feign

import feign.Response
import java.io.Reader

/**
 * [feign.Response.Builder]를 빌드합니다.
 *
 * @param builder [feign.Response.Builder]를 초기화하는 함수
 * @return [feign.Response] 인스턴스
 */
inline fun feignResponseBuilder(builder: Response.Builder.() -> Unit): Response.Builder {
    return Response.builder().apply(builder)
}

/**
 * [feign.Response]를 빌드합니다.
 *
 * @param builder [feign.Response.Builder]를 초기화하는 함수
 * @return [feign.Response] 인스턴스
 */
inline fun feignResponse(builder: Response.Builder.() -> Unit): Response {
    return feignResponseBuilder(builder).build()
}

/**
 * [Response]가 JSON 형식인지 검사합니다.
 */
fun Response.isJsonBody(): Boolean {
    val contentType = headers()["Content-Type"] ?: headers()["content-type"]
    return contentType?.any { it.contains("application/json", true) } ?: false
}

/**
 * [Response]가 TEXT 형식인지 검사합니다.
 */
fun Response.isTextBody(): Boolean {
    val contentType = headers()["Content-Type"] ?: headers()["content-type"]
    return contentType?.any { it.contains("text/plain", true) } ?: false
}

/**
 * Response body 를 읽기위해 Reader 변환합니다.
 */
fun Response.bodyAsReader(): Reader {
    return body().asReader(charset())
}
