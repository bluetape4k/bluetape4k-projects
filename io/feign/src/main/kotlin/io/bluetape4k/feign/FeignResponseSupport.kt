package io.bluetape4k.feign

import feign.Response
import java.io.Reader

/**
 * [Response.Builder]를 생성하고 초기화 블록을 적용합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 builder를 생성합니다.
 * - [builder]에서 설정한 값은 그대로 반환 builder에 반영됩니다.
 *
 * ```kotlin
 * val b = feignResponseBuilder { status(200) }
 * // b != null
 * ```
 */
inline fun feignResponseBuilder(builder: Response.Builder.() -> Unit): Response.Builder {
    return Response.builder().apply(builder)
}

/**
 * [Response]를 생성합니다.
 *
 * ## 동작/계약
 * - [feignResponseBuilder] 결과에 `build()`를 호출합니다.
 *
 * ```kotlin
 * val response = feignResponse { status(200); reason("OK") }
 * // response.status() == 200
 * ```
 */
inline fun feignResponse(builder: Response.Builder.() -> Unit): Response {
    return feignResponseBuilder(builder).build()
}

/**
 * 응답 `Content-Type`이 JSON 계열인지 검사합니다.
 *
 * ## 동작/계약
 * - `application/json` 또는 `+json` 접미사를 JSON으로 판단합니다.
 * - `Content-Type` 헤더가 없으면 `false`를 반환합니다.
 *
 * ```kotlin
 * val json = response.isJsonBody()
 * // json == true
 * ```
 */
fun Response.isJsonBody(): Boolean {
    val contentTypes = contentTypeValues() ?: return false
    return contentTypes.any { value ->
        val normalized = value.lowercase()
        normalized.contains("application/json") || normalized.contains("+json")
    }
}

/**
 * 응답 `Content-Type`이 `text/plain`인지 검사합니다.
 *
 * ## 동작/계약
 * - 대소문자를 무시해 `text/plain` 포함 여부를 확인합니다.
 * - 헤더가 없으면 `false`를 반환합니다.
 *
 * ```kotlin
 * val text = response.isTextBody()
 * // text == true
 * ```
 */
fun Response.isTextBody(): Boolean {
    return contentTypeValues()?.any { it.contains("text/plain", true) } ?: false
}

/**
 * 응답 본문을 [Reader]로 반환합니다.
 *
 * ## 동작/계약
 * - 본문이 없으면 [IllegalStateException]이 발생합니다.
 * - 본문 charset 정보가 있으면 해당 charset으로 reader를 생성합니다.
 *
 * ```kotlin
 * val reader = response.bodyAsReader()
 * // reader.read() >= -1
 * ```
 */
fun Response.bodyAsReader(): Reader {
    val responseBody = body() ?: error("Response body is null.")
    return responseBody.asReader(charset())
}

private fun Response.contentTypeValues(): Collection<String>? {
    return headers().entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }?.value
}
