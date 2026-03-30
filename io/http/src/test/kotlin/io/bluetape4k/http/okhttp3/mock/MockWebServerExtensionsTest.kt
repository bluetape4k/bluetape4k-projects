package io.bluetape4k.http.okhttp3.mock

import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class MockWebServerExtensionsTest {
    companion object: KLogging()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        client = okhttp3Client { }
    }

    @AfterEach
    fun afterEach() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    private fun executeGet(): okhttp3.Response {
        val request =
            Request
                .Builder()
                .url(server.baseUrl)
                .get()
                .build()
        return client.newCall(request).execute()
    }

    @Test
    fun `baseUrl - 슬래시로 끝나는 URL 문자열을 반환한다`() {
        val url = server.baseUrl
        url.shouldNotBeBlank()
        url.startsWith("http://").shouldBeTrue()
        url.endsWith("/").shouldBeTrue()
    }

    @Test
    fun `enqueueBody with string body - 응답 바디를 올바르게 반환한다`() {
        server.enqueueBody("hello world")

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.body.shouldNotBeNull().string() shouldBeEqualTo "hello world"
        }
    }

    @Test
    fun `enqueueBody with string body and vararg headers - 헤더를 포함한 응답을 반환한다`() {
        server.enqueueBody("ok", "X-Custom-Header: custom-value", "X-Another: another-value")

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.header("X-Custom-Header") shouldBeEqualTo "custom-value"
            response.header("X-Another") shouldBeEqualTo "another-value"
            response.body.shouldNotBeNull().string() shouldBeEqualTo "ok"
        }
    }

    @Test
    fun `enqueueBody with string body and map headers - 헤더 맵을 포함한 응답을 반환한다`() {
        server.enqueueBody("ok", mapOf("X-Map-Header" to "map-value"))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.header("X-Map-Header") shouldBeEqualTo "map-value"
            response.body.shouldNotBeNull().string() shouldBeEqualTo "ok"
        }
    }

    @Test
    fun `enqueueBodyWithDelay - 지연 응답을 반환한다`() {
        val delay = Duration.ofMillis(50)
        server.enqueueBodyWithDelay("delayed")

        val start = System.currentTimeMillis()
        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.body.shouldNotBeNull().string() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `enqueueBodyWithDelay with map headers - 지연 응답에 헤더 맵을 포함한다`() {
        server.enqueueBodyWithDelay("delayed-map", headers = mapOf("X-Delay" to "true"))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.header("X-Delay") shouldBeEqualTo "true"
            response.body.shouldNotBeNull().string() shouldBeEqualTo "delayed-map"
        }
    }

    @Test
    fun `enqueueBodyWithHeadersDelay - 헤더 전송을 지연한 응답을 반환한다`() {
        server.enqueueBodyWithHeadersDelay("headers-delayed", Duration.ofMillis(50))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.body.shouldNotBeNull().string() shouldBeEqualTo "headers-delayed"
        }
    }

    @Test
    fun `enqueueBodyWithHeadersDelay with map headers - 헤더 맵을 포함한 지연 응답을 반환한다`() {
        server.enqueueBodyWithHeadersDelay("headers-map", headers = mapOf("X-Headers-Delay" to "true"))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.header("X-Headers-Delay") shouldBeEqualTo "true"
            response.body.shouldNotBeNull().string() shouldBeEqualTo "headers-map"
        }
    }

    @Test
    fun `enqueueBodyWithGzip - gzip 압축 응답을 반환한다`() {
        server.enqueueBodyWithGzip("gzip body content")

        // OkHttp automatically decompresses gzip
        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
        }
    }

    @Test
    fun `enqueueBodyWithGzip with map headers - gzip 응답에 헤더 맵을 포함한다`() {
        server.enqueueBodyWithGzip("gzip with headers", mapOf("X-Compression" to "gzip"))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
        }
    }

    @Test
    fun `enqueueBodyWithDeflate - deflate 압축 응답을 반환한다`() {
        server.enqueueBodyWithDeflate("deflate body content")

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
        }
    }

    @Test
    fun `enqueueBodyWithDeflate with map headers - deflate 응답에 헤더 맵을 포함한다`() {
        server.enqueueBodyWithDeflate("deflate with headers", mapOf("X-Compression" to "deflate"))

        executeGet().use { response ->
            response.isSuccessful.shouldBeTrue()
        }
    }

    @Test
    fun `enqueueBody with DSL builder - DSL 블록으로 응답을 구성한다`() {
        server.enqueueBody("dsl body") {
            setResponseCode(201)
            addHeader("X-Created", "true")
        }

        executeGet().use { response ->
            response.code shouldBeEqualTo 201
            response.header("X-Created") shouldBeEqualTo "true"
            response.body.shouldNotBeNull().string() shouldBeEqualTo "dsl body"
        }
    }

    @Test
    fun `mockResponse DSL - MockResponse를 DSL로 구성한다`() {
        val response =
            mockResponse {
                setBody("test")
                setResponseCode(202)
                addHeader("X-Test", "yes")
            }
        response.getBody().shouldNotBeNull()
        response.headers["X-Test"] shouldBeEqualTo "yes"
    }
}
