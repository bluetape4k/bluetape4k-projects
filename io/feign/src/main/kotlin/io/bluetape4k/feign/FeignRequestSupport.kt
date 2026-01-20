package io.bluetape4k.feign

import feign.Request
import feign.Request.HttpMethod
import feign.Request.Options
import feign.RequestTemplate
import java.nio.charset.Charset

@JvmField
val defaultRequestOptions = Options()

/**
 * [feign.Request.Options] 를 생성하는 함수
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
 * Feign [Request] 를 생성합니다.
 *
 * ```
 * val request = feignRequestOf("https://nghttp2.org/httpbin/get", HttpMethod.GET)
 * ```
 *
 * @param url URL
 * @param httpMetho HTTP Method
 * @param headers Header 정보
 * @param body Body 정보
 * @param charset Charset 정보
 * @param requestTemplate Request Template 정보
 * @return [Request] 인스턴스
 */
fun feignRequestOf(
    url: String,
    httpMetho: HttpMethod = HttpMethod.GET,
    headers: Map<String, Collection<String>> = emptyMap(),
    body: ByteArray? = null,
    charset: Charset = Charsets.UTF_8,
    requestTemplate: RequestTemplate? = null,
): Request {
    return Request.create(httpMetho, url, headers, body, charset, requestTemplate)
}
