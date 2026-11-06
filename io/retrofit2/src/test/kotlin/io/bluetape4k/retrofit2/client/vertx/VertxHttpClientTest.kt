package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.CallFactoryConformanceTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VertxHttpClientTest: CallFactoryConformanceTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = vertxCallFactoryOf()

    override fun callFactory(callTimeout: Duration): Call.Factory =
        vertxCallFactoryOf(callTimeout = callTimeout)

    @Test
    fun `execute waits for callEnd listener before returning`() {
        val server = MockWebServer().apply {
            start()
            enqueue(MockResponse().setBody("ok"))
        }
        val callEndEntered = CountDownLatch(1)
        val releaseCallEnd = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val call = callFactory.newCall(Request.Builder().url(server.url("/")).build())
            call.addEventListener(object: EventListener() {
                override fun callEnd(call: Call) {
                    callEndEntered.countDown()
                    releaseCallEnd.await(5, TimeUnit.SECONDS).shouldBeTrue()
                }
            })

            val responseFuture = executor.submit<okhttp3.Response> { call.execute() }

            callEndEntered.await(5, TimeUnit.SECONDS).shouldBeTrue()
            responseFuture.isDone.shouldBeFalse()

            releaseCallEnd.countDown()
            responseFuture.get(5, TimeUnit.SECONDS).use { response ->
                response.body.string() shouldBeEqualTo "ok"
            }
        } finally {
            releaseCallEnd.countDown()
            executor.shutdownNow()
            server.shutdown()
        }
    }
}
