package io.bluetape4k.http.okhttp3

import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.http.okhttp3.mock.enqueueBodyWithHeadersDelay
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class OkHttp3ClientExtensionsCoroutinesTest {

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

    @Test
    fun `okhttp3RequestOf with vararg headers applies all headers`() {
        val request = okhttp3RequestOf(
            url = "${server.baseUrl}todos",
            "X-App-Key", "app-123", "X-Trace-Id", "trace-1"
        ) {
            get()
        }

        request.header("X-App-Key") shouldBeEqualTo "app-123"
        request.header("X-Trace-Id") shouldBeEqualTo "trace-1"
    }

    @Test
    fun `OkHttpClient executeSuspending returns successful response`() = runSuspendIO {
        val body = """{"result":"ok"}"""

        server.enqueueBody(body, "Content-Type: application/json")

        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        client.executeSuspending(request).use { response ->
            response.isSuccessful.shouldBeTrue()
            response.bodyAsString() shouldBeEqualTo body
        }
    }

    @Test
    fun `Call executeSuspending returns successful response`() = runSuspendIO {
        server.enqueueBody("ok")

        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        val call = client.newCall(request)

        call.executeSuspending().use { response ->
            response.isSuccessful.shouldBeTrue()
            response.bodyAsString() shouldBeEqualTo "ok"
        }
    }

    @Test
    fun `deprecated suspendExecute api is still supported`() = runSuspendIO {
        server.enqueueBody("legacy")

        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        client.executeSuspending(request).use { response ->
            response.isSuccessful.shouldBeTrue()
            response.bodyAsString() shouldBeEqualTo "legacy"
        }
    }

    @Test
    fun `Call executeSuspending 는 코루틴 취소 시 okhttp call 을 취소한다`() = runSuspendIO {
        server.enqueueBodyWithHeadersDelay(
            "delayed",
            java.time.Duration.ofMillis(500)
        )

        val request = okhttp3Request {
            url(server.baseUrl)
            get()
        }

        val call = client.newCall(request)
        val job = async {
            call.executeSuspending()
        }

        delay(10.milliseconds)
        job.cancel()

        await atMost 5.seconds untilSuspending { job.isCancelled && call.isCanceled() }

        call.isCanceled().shouldBeTrue()
        job.isCancelled.shouldBeTrue()
        job.isCompleted.shouldBeTrue()
    }
}
