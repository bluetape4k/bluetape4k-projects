package io.bluetape4k.feign

import feign.Response
import java.io.Reader

/**
 * [Response.Builder]를 생성하고 설정 블록을 적용합니다.
 *
 * @param builder [Response.Builder] 초기화 블록
 * @return 초기화된 [Response.Builder]
 */
inline fun feignResponseBuilder(builder: Response.Builder.() -> Unit): Response.Builder {
    return Response.builder().apply(builder)
}

/**
 * [Response]를 생성합니다.
 *
 * @param builder [Response.Builder] 초기화 블록
 * @return 생성된 [Response]
 */
inline fun feignResponse(builder: Response.Builder.() -> Unit): Response {
    return feignResponseBuilder(builder).build()
}

/**
 * [Response]의 `Content-Type`이 JSON인지 검사합니다.
 */
fun Response.isJsonBody(): Boolean {
    val contentTypes = contentTypeValues() ?: return false
    return contentTypes.any { value ->
        val normalized = value.lowercase()
        normalized.contains("application/json") || normalized.contains("+json")
    }
}

/**
 * [Response]의 `Content-Type`이 텍스트인지 검사합니다.
 */
fun Response.isTextBody(): Boolean {
    return contentTypeValues()?.any { it.contains("text/plain", true) } ?: false
}

/**
 * 응답 본문을 [Reader]로 변환합니다.
 *
 * 본문이 없는 응답에서 호출되면 [IllegalStateException]을 발생시킵니다.
 */
fun Response.bodyAsReader(): Reader {
    val responseBody = body() ?: error("Response body is null.")
    return responseBody.asReader(charset())
}

private fun Response.contentTypeValues(): Collection<String>? {
    return headers().entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }?.value
}
