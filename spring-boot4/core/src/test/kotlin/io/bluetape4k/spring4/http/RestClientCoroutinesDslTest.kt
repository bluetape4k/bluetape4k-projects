package io.bluetape4k.spring4.http

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class RestClientCoroutinesDslTest {
    companion object: KLogging()

    private lateinit var mockServer: MockWebServer
    private lateinit var restClient: RestClient

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        restClient = restClientOf(mockServer.url("/").toString())
    }

    @AfterEach
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `suspendGet returns deserialized response`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("hello")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String = restClient.suspendGet("/test")
            result shouldBeEqualTo "hello"
        }

    @Test
    fun `suspendPost returns deserialized response`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("created")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String = restClient.suspendPost("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "created"
        }

    @Test
    fun `suspendPut returns deserialized response`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("updated")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String = restClient.suspendPut("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "updated"
        }

    @Test
    fun `suspendPatch returns deserialized response`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("patched")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String = restClient.suspendPatch("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "patched"
        }

    @Test
    fun `suspendDelete completes without error`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setResponseCode(204)
            )
            restClient.suspendDelete("/test")
        }
}
