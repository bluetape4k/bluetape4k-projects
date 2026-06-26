package io.bluetape4k.http.okhttp3

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LoggingInterceptorTest {
    companion object: KLogging()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var appender: InMemoryLogbackAppender

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        val loggerName = LoggingInterceptorTest::class.java.name
        appender = InMemoryLogbackAppender(loggerName)
        val logger = LoggerFactory.getLogger(loggerName)
        client =
            OkHttpClient
                .Builder()
                .addInterceptor(LoggingInterceptor(logger))
                .build()
    }

    @AfterEach
    fun afterEach() {
        runCatching { appender.close() }
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

    @Test
    fun `LoggingInterceptor - sensitive request and response headers are redacted`() {
        val secretToken = "Bearer request-secret"
        val cookie = "session=request-cookie"
        val responseCookie = "session=response-cookie"
        val apiKey = "response-api-key"

        server.enqueue(
            MockResponse()
                .setBody("ok")
                .setResponseCode(200)
                .setHeader("Set-Cookie", responseCookie)
                .setHeader("X-Api-Key", apiKey)
                .setHeader("X-Trace-Id", "trace-123")
        )

        val request =
            Request
                .Builder()
                .url(server.url("/redaction"))
                .get()
                .header("Authorization", secretToken)
                .header("Cookie", cookie)
                .header("X-Request-Id", "request-123")
                .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful.shouldBeTrue()
        }

        val messages = appender.messages.joinToString("\n")
        messages shouldContain "Authorization: <redacted>"
        messages shouldContain "Cookie: <redacted>"
        messages shouldContain "Set-Cookie: <redacted>"
        messages shouldContain "X-Api-Key: <redacted>"
        messages shouldContain "X-Request-Id: request-123"
        messages shouldContain "X-Trace-Id: trace-123"
        messages shouldNotContain secretToken
        messages shouldNotContain cookie
        messages shouldNotContain responseCookie
        messages shouldNotContain apiKey
    }

    @Test
    fun `LoggingInterceptor - additional sensitive headers are redacted`() {
        val internalSecret = "internal-secret"
        val loggerName = LoggingInterceptorTest::class.java.name
        val customClient =
            OkHttpClient
                .Builder()
                .addInterceptor(LoggingInterceptor(LoggerFactory.getLogger(loggerName), setOf("X-Internal-Secret")))
                .build()

        server.enqueue(MockResponse().setBody("ok").setResponseCode(200))

        val request =
            Request
                .Builder()
                .url(server.url("/custom-redaction"))
                .get()
                .header("X-Internal-Secret", internalSecret)
                .header("X-Request-Id", "request-123")
                .build()

        customClient.newCall(request).execute().use { response ->
            response.isSuccessful.shouldBeTrue()
        }

        val messages = appender.messages.joinToString("\n")
        messages shouldContain "X-Internal-Secret: <redacted>"
        messages shouldContain "X-Request-Id: request-123"
        messages shouldNotContain internalSecret
    }
}
