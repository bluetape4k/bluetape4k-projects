package io.bluetape4k.feign

import feign.Request
import feign.Request.HttpMethod
import feign.Request.Options
import feign.RequestTemplate
import java.nio.charset.Charset

@JvmField
val defaultRequestOptions = Options()

/**
 * [Options]를 생성하고 설정 블록을 적용합니다.
 *
 * ```
 * val requestOptions = requestOptions {
 *      connectTimeout(1000)
 *      readTimeout(1000)
 * }
 * ```
 *
 * @param builder [feign.Request.Options]를 초기화하는 함수
 * @return [feign.Request.Options] 인스턴스
 */
inline fun requestOptions(builder: Options.() -> Unit): Options {
    return Options().apply(builder)
}

/**
 * Feign [Request]를 생성합니다.
 *
 * ```
 * val request = feignRequestOf("https://nghttp2.org/httpbin/get", HttpMethod.GET)
 * ```
 *
 * @param url URL
 * @param httpMethod HTTP 메서드
 * @param headers 요청 헤더
 * @param body 요청 본문
 * @param charset 본문 문자셋
 * @param requestTemplate 요청 템플릿
 * @return [Request] 인스턴스
 */
fun feignRequestOf(
    url: String,
    httpMethod: HttpMethod = HttpMethod.GET,
    headers: Map<String, Collection<String>> = emptyMap(),
    body: ByteArray? = null,
    charset: Charset = Charsets.UTF_8,
    requestTemplate: RequestTemplate? = null,
): Request {
    return Request.create(httpMethod, url, headers, body, charset, requestTemplate)
}
