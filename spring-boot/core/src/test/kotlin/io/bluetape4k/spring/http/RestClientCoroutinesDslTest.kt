package io.bluetape4k.spring.http

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.AbstractClientHttpRequest
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

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

    @Test
    fun `suspendGet cancels delayed blocking response promptly`() =
        runSuspendIO {
            val executeStarted = CountDownLatch(1)
            val interrupted = AtomicBoolean(false)
            val interruptibleClient =
                RestClient
                    .builder()
                    .requestFactory(InterruptibleBlockingRequestFactory(executeStarted, interrupted))
                    .build()
            val failure = AtomicReference<Throwable?>()

            val job = launch {
                runCatching {
                    interruptibleClient.suspendGet<String>("/slow")
                }.onFailure {
                    failure.set(it)
                }
            }

            executeStarted.await(1, TimeUnit.SECONDS) shouldBeEqualTo true
            withTimeout(2.seconds) {
                job.cancelAndJoin()
            }
            interrupted.get() shouldBeEqualTo true
            failure.get()?.cause?.cause?.message shouldBeEqualTo "blocking request interrupted"
        }

    @Test
    fun `suspendGetOrNull은 응답 본문이 있으면 역직렬화한다`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("hello")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String? = restClient.suspendGetOrNull("/test")
            result shouldBeEqualTo "hello"
        }

    @Test
    fun `suspendPostOrNull은 응답 본문이 있으면 역직렬화한다`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("created")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String? = restClient.suspendPostOrNull("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "created"
        }

    @Test
    fun `suspendPutOrNull은 응답 본문이 있으면 역직렬화한다`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("updated")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String? = restClient.suspendPutOrNull("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "updated"
        }

    @Test
    fun `suspendPatchOrNull은 응답 본문이 있으면 역직렬화한다`() =
        runTest {
            mockServer.enqueue(
                MockResponse()
                    .setBody("patched")
                    .addHeader("Content-Type", "text/plain")
            )
            val result: String? = restClient.suspendPatchOrNull("/test", "payload", MediaType.APPLICATION_JSON)
            result shouldBeEqualTo "patched"
        }

    private class InterruptibleBlockingRequestFactory(
        private val executeStarted: CountDownLatch,
        private val interrupted: AtomicBoolean,
    ): ClientHttpRequestFactory {

        override fun createRequest(
            uri: URI,
            httpMethod: HttpMethod,
        ): ClientHttpRequest =
            object: AbstractClientHttpRequest() {
                override fun getMethod(): HttpMethod = httpMethod

                override fun getURI(): URI = uri

                override fun getBodyInternal(headers: HttpHeaders): OutputStream =
                    ByteArrayOutputStream()

                override fun executeInternal(headers: HttpHeaders): ClientHttpResponse {
                    executeStarted.countDown()
                    return try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(30))
                        StringClientHttpResponse("too-late")
                    } catch (e: InterruptedException) {
                        interrupted.set(true)
                        Thread.currentThread().interrupt()
                        throw IOException("blocking request interrupted", e)
                    }
                }
            }
    }

    private class StringClientHttpResponse(
        private val body: String,
    ): ClientHttpResponse {
        private val headers = HttpHeaders().apply {
            contentType = MediaType.TEXT_PLAIN
        }

        override fun getStatusCode() = org.springframework.http.HttpStatus.OK

        override fun getStatusText(): String = "OK"

        override fun getHeaders(): HttpHeaders = headers

        override fun getBody() = ByteArrayInputStream(body.toByteArray())

        override fun close() = Unit
    }
}
