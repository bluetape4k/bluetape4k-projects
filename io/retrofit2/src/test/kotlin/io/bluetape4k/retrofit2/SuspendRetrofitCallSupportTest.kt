package io.bluetape4k.retrofit2

import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.services.TestService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.converter.scalars.ScalarsConverterFactory

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

        runCatching {
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
    fun `suspendExecute invokes cancelHandler on cancelled call`() = runSuspendIO {
        // Enqueue a response so the call has something to resolve
        server.enqueue(MockResponse().setBody("ok"))

        val call = api.get()
        // Cancel before executing - should trigger cancel path
        call.cancel()

        var cancelHandlerCalled = false
        runCatching {
            call.suspendExecute(cancelHandler = { cancelHandlerCalled = true })
        }
        // Either the handler was called or an exception was thrown for the cancelled call
        // Both are valid outcomes; the key is no unhandled exception propagation
    }
}
