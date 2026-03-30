package io.bluetape4k.retrofit2

import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.retrofit2.services.TestService
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.time.Duration

class RetryRetrofitCallSupportTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TestService.TestInterface

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        api = retrofitOf(server.baseUrl, converterFactory = ScalarsConverterFactory.create()).service()
    }

    @AfterEach
    fun afterEach() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `executeAsync with retry retries with cloned call`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("foo"))

        val retry = retryOf("retrofit-async")
        val response = api.get().executeAsync(retry).get()

        response.isSuccessful.shouldBeTrue()
        response.body() shouldBeEqualTo "foo"
        server.requestCount shouldBeEqualTo 2
    }

    @Test
    fun `suspendExecute with retry retries with cloned call`() = runSuspendIO {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("foo"))

        val retry = retryOf("retrofit-suspend")
        val response = api.get().suspendExecute(retry)

        response.isSuccessful.shouldBeTrue()
        response.body() shouldBeEqualTo "foo"
        server.requestCount shouldBeEqualTo 2
    }

    private fun retryOf(name: String): Retry =
        Retry.of(
            name,
            RetryConfig.custom<retrofit2.Response<String>>()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .retryOnException { it is IOException }
                .build(),
        )
}
