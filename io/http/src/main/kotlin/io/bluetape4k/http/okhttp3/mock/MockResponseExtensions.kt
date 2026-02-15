package io.bluetape4k.http.okhttp3.mock

import okhttp3.mockwebserver.MockResponse

/**
 * 헤더 문자열 목록을 [MockResponse]에 추가합니다.
 *
 * @param headers 추가할 헤더 문자열 목록
 * @return 현재 [MockResponse]
 */
fun MockResponse.addHeaders(vararg headers: String): MockResponse = apply {
    headers.forEach { addHeader(it) }
}

/**
 * 헤더 맵을 [MockResponse]에 추가합니다.
 *
 * @param headers 추가할 헤더 맵
 * @return 현재 [MockResponse]
 */
fun MockResponse.addHeaders(headers: Map<String, Any>): MockResponse = apply {
    headers.forEach { (name, value) -> addHeader(name, value) }
}
