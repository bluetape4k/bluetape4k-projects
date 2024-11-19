package io.bluetape4k.http.okhttp3.mock

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.toUtf8Bytes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.time.Duration
import java.util.concurrent.TimeUnit

val MockWebServer.baseUrl: String get() = url("/").toString()

inline fun mockResponse(initializer: MockResponse.() -> Unit): MockResponse {
    return MockResponse().apply(initializer)
}

fun MockWebServer.enqueueBody(body: String?, vararg headers: String) {
    val response = mockResponse {
        setBody(body ?: EMPTY_STRING)
        addHeaders(*headers)
    }
    enqueue(response)
}

fun MockWebServer.enqueueBody(body: String?, headers: Map<String, Any>) {
    val response = mockResponse {
        setBody(body ?: EMPTY_STRING)
        addHeaders(headers)
    }
    enqueue(response)
}

fun MockWebServer.enqueueBodyWithDelay(
    body: String?,
    delay: Duration = Duration.ofMillis(10),
    vararg headers: String,
) {
    val response = mockResponse {
        setBody(body ?: EMPTY_STRING)
        setBodyDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(*headers)
    }

    enqueue(response)
}

fun MockWebServer.enqueueBodyWithDelay(
    body: String?,
    delay: Duration = Duration.ofMillis(10),
    headers: Map<String, Any>,
) {
    val response = mockResponse {
        setBody(body ?: EMPTY_STRING)
        setBodyDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
        addHeaders(headers)
    }
    enqueue(response)
}

fun MockWebServer.enqueueBodyWithGzip(body: String?, vararg headers: String) {
    val response = mockResponse {
        addHeaders(*headers)
        addHeader("Content-Encoding", "gzip")
        setBody(Buffer().write(Compressors.GZip.compress(body?.toUtf8Bytes())))
    }
    enqueue(response)
}

fun MockWebServer.enqueueBodyWithGzip(body: String?, headers: Map<String, Any>) {
    val response = mockResponse {
        addHeaders(headers)
        addHeader("Content-Encoding", "gzip")
        setBody(Buffer().write(Compressors.GZip.compress(body?.toUtf8Bytes())))
    }
    enqueue(response)
}

fun MockWebServer.enqueueBodyWithDeflate(body: String?, vararg headers: String) {
    val response = mockResponse {
        addHeaders(*headers)
        addHeader("Content-Encoding", "deflate")
        setBody(Buffer().write(Compressors.Deflate.compress(body?.toUtf8Bytes())))
    }
    enqueue(response)
}

fun MockWebServer.enqueueBodyWithDeflate(body: String?, headers: Map<String, Any>) {
    val response = mockResponse {
        addHeaders(headers)
        addHeader("Content-Encoding", "deflate")
        setBody(Buffer().write(Compressors.Deflate.compress(body?.toUtf8Bytes())))
    }
    enqueue(response)
}

fun MockResponse.addHeaders(vararg headers: String): MockResponse = apply {
    headers.forEach { addHeader(it) }
}

fun MockResponse.addHeaders(headers: Map<String, Any>): MockResponse = apply {
    headers.forEach { (name, value) -> addHeader(name, value) }
}
