package io.bluetape4k.http.okhttp3.mock

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.toUtf8Bytes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * [MockWebServer]의 기본 URL 문자열을 반환합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.start()
 * val baseUrl = server.baseUrl
 * // "http://localhost:PORT/"
 * server.shutdown()
 * ```
 */
val MockWebServer.baseUrl: String get() = url("/").toString()

/**
 * 문자열 바디와 DSL로 설정한 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBody("""{"key":"value"}""") {
 *     setResponseCode(200)
 *     addHeader("Content-Type", "application/json")
 * }
 * server.start()
 * ```
 *
 * @param body 응답 바디 문자열
 * @param builder [MockResponse] 설정 블록
 */
inline fun MockWebServer.enqueueBody(
    body: String,
    builder: MockResponse.() -> Unit,
) {
    val response = mockResponse {
        setBody(body)
        builder()
    }
    enqueue(response)
}

/**
 * [Buffer] 바디와 DSL로 설정한 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * val buffer = Buffer().writeUtf8("""{"key":"value"}""")
 * server.enqueueBody(buffer) {
 *     setResponseCode(200)
 *     addHeader("Content-Type", "application/json")
 * }
 * server.start()
 * ```
 *
 * @param bodyBuffer 응답 바디 [Buffer]
 * @param builder [MockResponse] 설정 블록
 */
inline fun MockWebServer.enqueueBody(
    bodyBuffer: Buffer,
    builder: MockResponse.() -> Unit,
) {
    val response = mockResponse {
        setBody(bodyBuffer)
        builder()
    }
    enqueue(response)
}

/**
 * 문자열 바디와 헤더를 가진 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBody("Hello", "Content-Type: text/plain", "X-Custom: value")
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBody(body: String, vararg headers: String) {
    enqueueBody(body) {
        addHeaders(*headers)
    }
}

/**
 * 문자열 바디와 헤더를 가진 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBody("Hello", mapOf("Content-Type" to "text/plain", "X-Version" to 1))
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBody(body: String, headers: Map<String, Any>) {
    enqueueBody(body) {
        addHeaders(headers)
    }
}

/**
 * 지연 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithDelay(
 *     body = """{"status":"ok"}""",
 *     delay = Duration.ofMillis(100),
 *     "Content-Type: application/json"
 * )
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param delay 바디 전송 지연 시간
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBodyWithDelay(
    body: String,
    delay: Duration = Duration.ofMillis(10),
    vararg headers: String,
) {
    enqueueBody(body) {
        setBodyDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(*headers)
    }
}

/**
 * 지연 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithDelay(
 *     body = """{"status":"ok"}""",
 *     delay = Duration.ofMillis(100),
 *     headers = mapOf("Content-Type" to "application/json")
 * )
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param delay 바디 전송 지연 시간
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBodyWithDelay(
    body: String,
    delay: Duration = Duration.ofMillis(10),
    headers: Map<String, Any>,
) {
    enqueueBody(body) {
        setBodyDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(headers)
    }
}

/**
 * 헤더 전송을 지연한 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithHeadersDelay(
 *     body = "Hello",
 *     delay = Duration.ofMillis(50),
 *     "Content-Type: text/plain"
 * )
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param delay 헤더 전송 지연 시간
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBodyWithHeadersDelay(
    body: String,
    delay: Duration = Duration.ofMillis(10),
    vararg headers: String,
) {
    enqueueBody(body) {
        setHeadersDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(*headers)
    }
}

/**
 * 헤더 전송을 지연한 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithHeadersDelay(
 *     body = "Hello",
 *     delay = Duration.ofMillis(50),
 *     headers = mapOf("Content-Type" to "text/plain")
 * )
 * server.start()
 * ```
 *
 * @param body 응답 바디
 * @param delay 헤더 전송 지연 시간
 * @param headers 응답 헤더
 */
fun MockWebServer.enqueueBodyWithHeadersDelay(
    body: String,
    delay: Duration = Duration.ofMillis(10),
    headers: Map<String, Any>,
) {
    enqueueBody(body) {
        setHeadersDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(headers)
    }
}

/**
 * gzip 압축 바디 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithGzip(
 *     body = """{"data":"hello"}""",
 *     "Content-Type: application/json"
 * )
 * server.start()
 * // 응답에 Content-Encoding: gzip 헤더가 포함됩니다.
 * ```
 *
 * @param body 원본 바디
 * @param headers 추가 헤더
 */
fun MockWebServer.enqueueBodyWithGzip(body: String, vararg headers: String) {
    val compressed = Buffer().write(Compressors.GZip.compress(body.toUtf8Bytes()))
    enqueueBody(compressed) {
        addHeaders(*headers)
        addHeader("Content-Encoding", "gzip")
    }
}

/**
 * gzip 압축 바디 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithGzip(
 *     body = """{"data":"hello"}""",
 *     headers = mapOf("Content-Type" to "application/json")
 * )
 * server.start()
 * // 응답에 Content-Encoding: gzip 헤더가 포함됩니다.
 * ```
 *
 * @param body 원본 바디
 * @param headers 추가 헤더
 */
fun MockWebServer.enqueueBodyWithGzip(body: String, headers: Map<String, Any>) {
    val compressed = Buffer().write(Compressors.GZip.compress(body.toUtf8Bytes()))
    enqueueBody(compressed) {
        addHeader("Content-Encoding", "gzip")
        addHeaders(headers)
    }
}

/**
 * deflate 압축 바디 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithDeflate(
 *     body = "Hello World",
 *     "Content-Type: text/plain"
 * )
 * server.start()
 * // 응답에 Content-Encoding: deflate 헤더가 포함됩니다.
 * ```
 *
 * @param body 원본 바디
 * @param headers 추가 헤더
 */
fun MockWebServer.enqueueBodyWithDeflate(body: String, vararg headers: String) {
    val compressed = Buffer().write(Compressors.Deflate.compress(body.toUtf8Bytes()))
    enqueueBody(compressed) {
        addHeader("Content-Encoding", "deflate")
        addHeaders(*headers)
    }
}

/**
 * deflate 압축 바디 응답을 enqueue 합니다.
 *
 * ```kotlin
 * val server = MockWebServer()
 * server.enqueueBodyWithDeflate(
 *     body = "Hello World",
 *     headers = mapOf("Content-Type" to "text/plain")
 * )
 * server.start()
 * // 응답에 Content-Encoding: deflate 헤더가 포함됩니다.
 * ```
 *
 * @param body 원본 바디
 * @param headers 추가 헤더
 */
fun MockWebServer.enqueueBodyWithDeflate(body: String, headers: Map<String, Any>) {
    val compressed = Buffer().write(Compressors.Deflate.compress(body.toUtf8Bytes()))
    enqueueBody(compressed) {
        addHeader("Content-Encoding", "deflate")
        addHeaders(headers)
    }
}
