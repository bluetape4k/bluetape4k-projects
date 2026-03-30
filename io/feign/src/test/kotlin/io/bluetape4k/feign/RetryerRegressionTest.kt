package io.bluetape4k.feign

import feign.Param
import feign.RequestLine
import feign.Retryer
import feign.hc5.ApacheHttp5Client
import io.bluetape4k.http.okhttp3.mock.baseUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RetryerRegressionTest {

    private interface RetryApi {
        @RequestLine("GET /retry/{id}")
        fun get(@Param("id") id: Int): String
    }

    private lateinit var server: MockWebServer
    private lateinit var api: RetryApi

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        api = feignBuilder {
            client(ApacheHttp5Client())
            retryer(Retryer.Default(0, 0, 2))
        }.client(server.baseUrl)
    }

    @AfterEach
    fun afterEach() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `feign retryer retries disconnected request and succeeds on next response`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("ok"))

        val result = api.get(1)

        result shouldBeEqualTo "ok"
        server.requestCount shouldBeEqualTo 2
    }
}
