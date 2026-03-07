package io.bluetape4k.http.okhttp3

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OkHttpResponseExtensionsTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun cleanup() {
        runCatching { server.shutdown() }
    }

    private fun enqueueAndExecute(body: String, contentType: String = "text/plain"): okhttp3.Response {
        server.enqueue(MockResponse().setBody(body).addHeader("Content-Type", contentType))
        val request = Request.Builder().url(server.url("/")).get().build()
        return client.newCall(request).execute()
    }

    @Test
    fun `bodyAsString - 응답 바디를 문자열로 반환한다`() {
        val response = enqueueAndExecute("Hello, World!")
        response.bodyAsString() shouldBeEqualTo "Hello, World!"
    }

    @Test
    fun `bodyAsByteArray - 응답 바디를 ByteArray로 반환한다`() {
        val response = enqueueAndExecute("bytes")
        val bytes = response.bodyAsByteArray()
        bytes.shouldNotBeNull()
        bytes.shouldNotBeEmpty()
        String(bytes) shouldBeEqualTo "bytes"
    }

    @Test
    fun `bodyAsInputStream - 응답 바디를 InputStream으로 반환한다`() {
        val response = enqueueAndExecute("stream")
        val stream = response.bodyAsInputStream()
        stream.shouldNotBeNull()
        String(stream.readBytes()) shouldBeEqualTo "stream"
    }

    @Test
    fun `bodyAsString - null 응답에 대해 null을 반환한다`() {
        val nullResponse: okhttp3.Response? = null
        nullResponse.bodyAsString().shouldBeNull()
    }

    @Test
    fun `bodyAsByteArray - null 응답에 대해 null을 반환한다`() {
        val nullResponse: okhttp3.Response? = null
        nullResponse.bodyAsByteArray().shouldBeNull()
    }

    @Test
    fun `bodyAsInputStream - null 응답에 대해 null을 반환한다`() {
        val nullResponse: okhttp3.Response? = null
        nullResponse.bodyAsInputStream().shouldBeNull()
    }

    @Test
    fun `print - 응답 정보를 예외 없이 로깅한다`() {
        val response = enqueueAndExecute("ok")
        // 예외 없이 실행되면 성공
        response.print(1)
        response.print(2)
    }

    @Test
    fun `toTypeString - MediaType을 type-slash-subtype 형식으로 반환한다`() {
        val mediaType = "application/json".toMediaType()
        mediaType.toTypeString() shouldBeEqualTo "application/json"
    }

    @Test
    fun `toTypeString - text plain MediaType 변환`() {
        val mediaType = "text/plain; charset=utf-8".toMediaType()
        mediaType.toTypeString() shouldBeEqualTo "text/plain"
    }
}
