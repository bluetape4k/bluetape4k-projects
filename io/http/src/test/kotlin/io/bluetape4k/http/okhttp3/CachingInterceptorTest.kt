package io.bluetape4k.http.okhttp3

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CachingInterceptorTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun cleanup() {
        server.shutdown()
    }

    @Test
    fun `request interceptor adds cache control when missing`() {
        server.enqueue(MockResponse().setBody("ok"))

        val client = OkHttpClient.Builder()
            .addInterceptor(CachingRequestInterceptor())
            .build()

        val request = Request.Builder()
            .url(server.url("/"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            response.body.shouldNotBeNull()
        }

        val recordedRequest = server.takeRequest()
        recordedRequest.getHeader("Cache-Control").shouldNotBeNull()
    }

    @Test
    fun `request interceptor preserves existing cache control`() {
        server.enqueue(MockResponse().setBody("ok"))

        val client = OkHttpClient.Builder()
            .addInterceptor(CachingRequestInterceptor())
            .build()

        val request = Request.Builder()
            .url(server.url("/"))
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            response.body.shouldNotBeNull()
        }

        val recordedRequest = server.takeRequest()
        recordedRequest.getHeader("Cache-Control") shouldBeEqualTo "no-cache"
    }

    @Test
    fun `response interceptor adds cache control when missing`() {
        server.enqueue(MockResponse().setBody("ok"))

        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(CachingResponseInterceptor(maxAgeInSeconds = 60))
            .build()

        val request = Request.Builder()
            .url(server.url("/"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            response.header("Cache-Control").shouldNotBeNull()
        }
    }

    @Test
    fun `response interceptor preserves existing cache control`() {
        server.enqueue(
            MockResponse()
                .setBody("ok")
                .addHeader("Cache-Control", "no-store")
        )

        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(CachingResponseInterceptor(maxAgeInSeconds = 60))
            .build()

        val request = Request.Builder()
            .url(server.url("/"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            response.header("Cache-Control") shouldBeEqualTo "no-store"
        }
    }
}
