package io.bluetape4k.mockserver.httpbin

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.mockserver.httpbin.model.HttpbinResponse
import jakarta.servlet.http.HttpServletRequest

private val objectMapper = ObjectMapper()
private val mapTypeRef = object: TypeReference<Map<String, Any>>() {}

/**
 * HttpServletRequest의 헤더를 Map으로 변환한다.
 *
 * @return 헤더 이름과 값의 Map
 */
fun HttpServletRequest.toHeaderMap(): Map<String, String> =
    headerNames.asSequence().associateWith { getHeader(it) }

/**
 * HttpServletRequest와 요청 바디를 HttpbinResponse로 변환한다.
 *
 * JSON Content-Type이면 body를 파싱해 `json` 필드에 담고,
 * form-urlencoded이면 `form` 필드에, 그 외는 `data` 필드에 담는다.
 *
 * @param body 요청 바디 문자열 (nullable)
 * @param method HTTP 메서드명
 * @return httpbin 형식의 응답 객체
 */
fun HttpServletRequest.toHttpbinResponse(body: String? = null, method: String? = null): HttpbinResponse {
    val args = queryString?.split("&")
        ?.filter { it.contains("=") }
        ?.associate { it.substringBefore("=") to it.substringAfter("=") }
        ?: emptyMap()

    val headers = toHeaderMap()
    val contentType = contentType ?: ""

    val isJson = contentType.contains("application/json", ignoreCase = true)
    val isForm = contentType.contains("application/x-www-form-urlencoded", ignoreCase = true)

    val formData: Map<String, String> = if (isForm && body != null) {
        body.split("&")
            .filter { it.contains("=") }
            .associate { it.substringBefore("=") to it.substringAfter("=") }
    } else emptyMap()

    val jsonData: Map<String, Any>? = if (isJson && !body.isNullOrBlank()) {
        runCatching { objectMapper.readValue(body, mapTypeRef) }.getOrNull()
    } else null

    return HttpbinResponse(
        args = args,
        headers = headers,
        origin = remoteAddr ?: "",
        url = requestURL.toString(),
        data = if (formData.isEmpty() && jsonData == null) body ?: "" else "",
        form = formData,
        json = jsonData,
        method = method ?: this.method,
    )
}
