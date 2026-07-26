package io.bluetape4k.retrofit2

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.junit5.coroutines.assertResourceCancelledOnCoroutineCancellation
import io.bluetape4k.junit5.coroutines.runCatchingNonCancellation
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.services.TestService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * [suspendExecute] 확장 함수 단위 테스트입니다.
 */
class SuspendRetrofitCallSupportTest {

    companion object: KLogging()

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
    fun `suspendExecute returns successful response body`() = runSuspendIO {
        server.enqueue(MockResponse().setBody("hello"))

        val response = api.get().suspendExecute()

        response.isSuccessful.shouldBeTrue()
        response.body() shouldBeEqualTo "hello"
    }

    @Test
    fun `suspendExecute with 200 ok returns body`() = runSuspendIO {
        server.enqueue(MockResponse().setResponseCode(200).setBody("success"))

        val response = api.get().suspendExecute()

        response.code() shouldBeEqualTo 200
        response.body().shouldNotBeNull()
    }

    @Test
    fun `suspendExecute with network error propagates exception`() = runSuspendIO {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        runCatchingNonCancellation {
            api.get().suspendExecute()
        }.isFailure.shouldBeTrue()
    }

    @Test
    fun `suspendExecute with 404 still returns response (non-2xx)`() = runSuspendIO {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val response = api.get().suspendExecute()

        // 4xx is a valid response (not a network failure) - isSuccessful == false
        response.isSuccessful shouldBeEqualTo false
        response.code() shouldBeEqualTo 404
    }

    @Test
    fun `suspendExecute with 500 still returns response`() = runSuspendIO {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))

        val response = api.get().suspendExecute()

        response.isSuccessful shouldBeEqualTo false
        response.code() shouldBeEqualTo 500
    }

    @Test
    fun `suspendExecute cancels underlying call when coroutine is cancelled`() = runSuspendIO {
        server.enqueue(
            MockResponse()
                .setBody("late")
                .setBodyDelay(5, TimeUnit.SECONDS)
        )

        lateinit var call: Call<String>

        assertResourceCancelledOnCoroutineCancellation(
            beforeCancel = {
                server.takeRequest(1, TimeUnit.SECONDS).shouldNotBeNull()
            },
            resourceCancelled = { call.isCanceled },
        ) {
            call = api.get()
            call.suspendExecute()
        }
    }

    @Test
    fun `suspendExecute closes successful response body when cancellation wins response race`() = runTest {
        val call = ControlledCall<String>()
        val cancelCause = AtomicReference<Throwable?>()
        val body = CloseTrackingResponseBody()

        val job = launch {
            runCatching {
                call.suspendExecute { cancelCause.set(it) }
            }
        }

        call.awaitEnqueued()
        job.cancel("cancel before response")
        call.callback.onResponse(call, successResponse("hello", body))
        job.join()

        call.isCanceled.shouldBeTrue()
        body.closed.shouldBeTrue()
        cancelCause.get().shouldNotBeNull()
    }

    @Test
    fun `suspendExecute closes error response body when cancellation wins response race`() = runTest {
        val call = ControlledCall<String>()
        val cancelCause = AtomicReference<Throwable?>()
        val body = CloseTrackingResponseBody()

        val job = launch {
            runCatching {
                call.suspendExecute { cancelCause.set(it) }
            }
        }

        call.awaitEnqueued()
        job.cancel("cancel before response")
        call.callback.onResponse(call, Response.error(500, body))
        job.join()

        call.isCanceled.shouldBeTrue()
        body.closed.shouldBeTrue()
        cancelCause.get().shouldNotBeNull()
    }

    private fun successResponse(
        body: String,
        rawBody: ResponseBody,
    ): Response<String> {
        val raw =
            okhttp3.Response
                .Builder()
                .request(Request.Builder().url("https://example.test/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(rawBody)
                .build()

        return Response.success(body, raw)
    }

    private class ControlledCall<T>: Call<T> {
        private val callbackReady = CompletableDeferred<Callback<T>>()
        private var canceled = false

        val callback: Callback<T>
            get() = callbackReady.getCompleted()

        suspend fun awaitEnqueued() {
            callbackReady.await()
        }

        override fun enqueue(callback: Callback<T>) {
            callbackReady.complete(callback)
        }

        override fun cancel() {
            canceled = true
        }

        override fun isCanceled(): Boolean = canceled

        override fun isExecuted(): Boolean = callbackReady.isCompleted

        override fun clone(): Call<T> = ControlledCall()

        override fun execute(): Response<T> = error("Synchronous execution is not used by this test.")

        override fun request(): Request =
            Request.Builder().url("https://example.test/").build()

        override fun timeout(): Timeout = Timeout.NONE
    }

    private class CloseTrackingResponseBody: ResponseBody() {
        private val source = CloseTrackingSource()
        val closed: Boolean get() = source.closed

        override fun contentType() = null

        override fun contentLength(): Long = 0L

        override fun source(): BufferedSource = source.buffer()
    }

    private class CloseTrackingSource: Source {
        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
        }

        override fun read(sink: Buffer, byteCount: Long): Long = -1L

        override fun timeout(): Timeout = Timeout.NONE
    }
}
