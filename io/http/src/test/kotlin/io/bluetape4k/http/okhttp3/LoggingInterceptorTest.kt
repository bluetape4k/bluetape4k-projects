package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LoggingInterceptorTest {
    companion object: KLogging()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        val logger = LoggerFactory.getLogger(LoggingInterceptorTest::class.java)
        client =
            OkHttpClient
                .Builder()
                .addInterceptor(LoggingInterceptor(logger))
                .build()
    }

    @AfterEach
    fun afterEach() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `LoggingInterceptor - 요청과 응답을 로깅하고 응답을 그대로 반환한다`() {
        server.enqueue(MockResponse().setBody("ok").setResponseCode(200))

        val request =
            Request
                .Builder()
                .url(server.url("/"))
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `LoggingInterceptor - 여러 요청에 대해 반복 동작한다`() {
        repeat(3) {
            server.enqueue(MockResponse().setBody("repeat-$it").setResponseCode(200))
        }

        repeat(3) {
            val request =
                Request
                    .Builder()
                    .url(server.url("/path-$it"))
                    .get()
                    .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful.shouldBeTrue()
            }
        }
    }
}
