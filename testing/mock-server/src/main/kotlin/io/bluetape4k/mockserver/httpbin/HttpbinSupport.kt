package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.mockserver.httpbin.model.HttpbinResponse
import jakarta.servlet.http.HttpServletRequest

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
    val formData = if (contentType.contains("application/x-www-form-urlencoded") && body != null) {
        body.split("&")
            .filter { it.contains("=") }
            .associate { it.substringBefore("=") to it.substringAfter("=") }
    } else emptyMap()

    return HttpbinResponse(
        args = args,
        headers = headers,
        origin = remoteAddr ?: "",
        url = requestURL.toString(),
        data = if (formData.isEmpty()) body else null,
        form = formData,
        method = method ?: this.method,
    )
}
