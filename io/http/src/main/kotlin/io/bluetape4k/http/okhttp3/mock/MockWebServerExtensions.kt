package io.bluetape4k.http.okhttp3.mock

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.toUtf8Bytes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.time.Duration
import java.util.concurrent.TimeUnit

/** [MockWebServer]의 기본 URL 문자열을 반환합니다. */
val MockWebServer.baseUrl: String get() = url("/").toString()

inline fun MockWebServer.enqueueBody(
    body: String,
    @BuilderInference builder: MockResponse.() -> Unit,
) {
    val response = mockResponse {
        setBody(body)
        builder()
    }
    enqueue(response)
}

inline fun MockWebServer.enqueueBody(
    bodyBuffer: Buffer,
    @BuilderInference builder: MockResponse.() -> Unit,
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
 * gzip 압축 바디 응답을 enqueue 합니다.
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
