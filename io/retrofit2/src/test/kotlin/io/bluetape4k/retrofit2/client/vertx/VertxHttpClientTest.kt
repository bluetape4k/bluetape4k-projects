package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractClientTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VertxHttpClientTest: AbstractClientTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = vertxCallFactoryOf()

    // --- cancel / tag contract regression tests (#484) ---

    private lateinit var cancelTestServer: MockWebServer

    @BeforeEach
    fun beforeCancelTest() {
        cancelTestServer = MockWebServer().apply { start() }
    }

    @AfterEach
    fun afterCancelTest() {
        runCatching { cancelTestServer.shutdown() }
    }

    @Test
    fun `cancel sets isCanceled to true immediately before execute`() {
        cancelTestServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val request = Request.Builder()
            .url(cancelTestServer.url("/"))
            .build()
        val call = callFactory.newCall(request)

        call.isCanceled().shouldBeFalse()
        call.cancel()
        call.isCanceled().shouldBeTrue()
    }

    /**
     * Verifies that cancel() propagates to the underlying Vert.x request via reset(), not just the CompletableFuture.
     *
     * Without underlying request cancellation the server never sends a response (NO_RESPONSE policy),
     * so onFailure would only be called after the 30s callTimeout. The latch would not be counted
     * down within 5 seconds, causing the test to fail. With cancellation propagated via
     * HttpClientRequest.reset(), the request is aborted immediately and onFailure fires within milliseconds.
     */
    @Test
    fun `cancel during enqueue propagates to underlying Vertx request and fires onFailure promptly`() {
        cancelTestServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val request = Request.Builder()
            .url(cancelTestServer.url("/"))
            .build()
        val call = callFactory.newCall(request)
        val latch = CountDownLatch(1)

        call.enqueue(object: okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                latch.countDown()
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
                latch.countDown()
            }
        })
        call.cancel()

        // If cancel propagates to the Vert.x request, onFailure fires well within 5 s.
        // Without propagation the request would hang for the full 30 s callTimeout.
        latch.await(5, TimeUnit.SECONDS).shouldBeTrue()
        call.isCanceled().shouldBeTrue()
    }

    @Test
    fun `tag computeIfAbsent caches and returns same instance on repeated calls`() {
        val request = Request.Builder()
            .url(cancelTestServer.url("/"))
            .build()
        val call = callFactory.newCall(request)

        data class MyTag(val value: String)

        val first = call.tag(MyTag::class) { MyTag("hello") }
        first.shouldNotBeNull()

        val second = call.tag(MyTag::class) { MyTag("world") }
        // computeIfAbsent: second call must return same cached instance
        second shouldBeSameInstanceAs first
    }

    @Test
    fun `tag read returns null when tag has not been set`() {
        val request = Request.Builder()
            .url(cancelTestServer.url("/"))
            .build()
        val call = callFactory.newCall(request)

        data class UnsetTag(val x: Int)

        call.tag(UnsetTag::class).shouldBeNull()
    }
}
