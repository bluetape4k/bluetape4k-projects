package io.bluetape4k.http.okhttp3.mock

import okhttp3.mockwebserver.MockResponse

/**
 * [MockResponse]를 생성합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * val response = mockResponse {
 *     setResponseCode(200)
 *     setBody("""{"status": "ok"}""")
 *     addHeader("Content-Type", "application/json")
 * }
 * server.enqueue(response)
 * ```
 *
 * @param builder [MockResponse] 초기화 블록
 * @return 생성된 [MockResponse]
 */
inline fun mockResponse(builder: MockResponse.() -> Unit): MockResponse =
    MockResponse().apply(builder)
