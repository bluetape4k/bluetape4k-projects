package io.bluetape4k.feign.clients

import feign.AsyncClient
import feign.Client
import feign.Request
import feign.Response
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.feignRequestOf
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.toUtf8String
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Shared conformance tests for synchronous Feign HTTP transport adapters.
 */
abstract class FeignSyncClientConformanceTest: AbstractFeignTest() {

    private lateinit var server: MockWebServer
    private lateinit var client: Client

    protected abstract fun newClient(): Client

    @BeforeEach
    fun startServer() {
        server = MockWebServer().apply { start() }
        client = newClient()
    }

    @AfterEach
    fun stopServer() {
        (client as? AutoCloseable)?.closeSafe()
        server.closeSafe()
    }

    @Test
    fun `delayed response completes and body can be closed`() {
        server.enqueue(
            MockResponse()
                .setBody("delayed")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        )

        client.execute(request(), defaultOptions()).use { response ->
            response.status() shouldBeEqualTo 200
            response.body().asInputStream().readBytes().toUtf8String() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `read timeout completes with failure instead of hanging`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = assertFailsWith<Exception> {
            client.execute(request(), timeoutOptions())
        }
        error.shouldNotBeNull()
    }

    private fun request(): Request =
        feignRequestOf(server.url("/").toString())
}

/**
 * Shared conformance tests for asynchronous Feign HTTP transport adapters.
 */
abstract class FeignAsyncClientConformanceTest<C: Any>: AbstractFeignTest() {

    private lateinit var server: MockWebServer
    private lateinit var client: AsyncClient<C>

    protected abstract fun newAsyncClient(): AsyncClient<C>

    protected abstract fun requestContext(): Optional<C>

    @BeforeEach
    fun startServer() {
        server = MockWebServer().apply { start() }
        client = newAsyncClient()
    }

    @AfterEach
    fun stopServer() {
        (client as? AutoCloseable)?.closeSafe()
        server.closeSafe()
    }

    @Test
    fun `delayed async response completes and body can be closed`() {
        server.enqueue(
            MockResponse()
                .setBody("delayed")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        )

        executeAsync(defaultOptions()).get(2, TimeUnit.SECONDS).use { response ->
            response.status() shouldBeEqualTo 200
            response.body().asInputStream().readBytes().toUtf8String() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `async read timeout completes exceptionally instead of hanging`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = assertFailsWith<ExecutionException> {
            executeAsync(timeoutOptions()).get(2, TimeUnit.SECONDS)
        }
        error.cause.shouldNotBeNull()
    }

    @Test
    fun `cancel marks async response future cancelled`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val future = executeAsync(timeoutOptions())

        future.cancel(true) shouldBeEqualTo true
        future.isCancelled shouldBeEqualTo true
    }

    private fun executeAsync(options: Request.Options): CompletableFuture<Response> =
        client.execute(
            feignRequestOf(server.url("/").toString()),
            options,
            requestContext()
        )
}

private fun defaultOptions(): Request.Options =
    Request.Options(1, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true)

private fun timeoutOptions(): Request.Options =
    Request.Options(100, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS, true)
