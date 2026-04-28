package io.bluetape4k.feign

import feign.Request
import feign.Request.HttpMethod
import feign.Request.Options
import feign.RequestTemplate
import io.bluetape4k.support.requireNotBlank
import java.nio.charset.Charset

/**
 * Feign 기본 요청 옵션입니다.
 *
 * ## 동작/계약
 * - [Options] 기본 생성자로 만든 공유 singleton 인스턴스입니다.
 * - connect/read timeout 등은 Feign 기본값을 따릅니다.
 *
 * ## 주의사항 (스레드 안전성)
 * - 이 인스턴스는 **읽기 전용으로만 사용**해야 합니다. 직접 변경하면 race condition이 발생할 수 있습니다.
 * - timeout, redirect 정책 등을 변경해야 하는 경우 [requestOptions] 팩토리를 사용해 매 호출마다
 *   새 [Options] 인스턴스를 생성하십시오.
 *
 * ```kotlin
 * // 기본값 그대로 읽기 전용 사용 — safe
 * val options = defaultRequestOptions
 *
 * // 커스텀 옵션이 필요한 경우 — requestOptions 팩토리 사용
 * val options = requestOptions { readTimeout(1000); connectTimeout(1000) }
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
 * - [url]은 blank가 아니어야 합니다. blank면 [IllegalArgumentException]이 발생합니다.
 * - 전달한 [url], [httpMethod], [headers], [body], [charset], [requestTemplate]를 그대로 사용합니다.
 * - [body]가 `null`이면 본문 없는 요청이 생성됩니다.
 *
 * ```kotlin
 * val request = feignRequestOf("https://example.com/health", HttpMethod.GET)
 * // request.httpMethod() == HttpMethod.GET
 * ```
 *
 * @param url 요청 대상 URL입니다. blank면 예외가 발생합니다.
 */
fun feignRequestOf(
    url: String,
    httpMethod: HttpMethod = HttpMethod.GET,
    headers: Map<String, Collection<String>> = emptyMap(),
    body: ByteArray? = null,
    charset: Charset = Charsets.UTF_8,
    requestTemplate: RequestTemplate? = null,
): Request {
    url.requireNotBlank("url")
    return Request.create(httpMethod, url, headers, body, charset, requestTemplate)
}
