package io.bluetape4k.feign

import feign.Request
import feign.Request.HttpMethod
import feign.Request.Options
import feign.RequestTemplate
import java.nio.charset.Charset

/**
 * Feign 기본 요청 옵션입니다.
 *
 * ## 동작/계약
 * - [Options] 기본 생성자로 만든 singleton 인스턴스입니다.
 * - connect/read timeout 등은 Feign 기본값을 따릅니다.
 *
 * ```kotlin
 * val options = defaultRequestOptions
 * // options != null
 * ```
 */
@JvmField
val defaultRequestOptions = Options()

/**
 * [Options]를 생성하고 설정 블록을 적용합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 [Options] 인스턴스를 생성합니다.
 * - [builder] 설정을 반환 객체에 반영합니다.
 *
 * ```kotlin
 * val options = requestOptions { readTimeout(1000); connectTimeout(1000) }
 * // options.readTimeoutMillis() == 1000
 * ```
 */
inline fun requestOptions(builder: Options.() -> Unit): Options {
    return Options().apply(builder)
}

/**
 * Feign [Request]를 생성합니다.
 *
 * ## 동작/계약
 * - 전달한 [url], [httpMethod], [headers], [body], [charset], [requestTemplate]를 그대로 사용합니다.
 * - [body]가 `null`이면 본문 없는 요청이 생성됩니다.
 *
 * ```kotlin
 * val request = feignRequestOf("https://example.com/health", HttpMethod.GET)
 * // request.httpMethod() == HttpMethod.GET
 * ```
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
