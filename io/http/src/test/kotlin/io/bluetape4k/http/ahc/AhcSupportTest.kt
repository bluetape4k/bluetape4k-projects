package io.bluetape4k.http.ahc

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.BoundRequestBuilder
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.RequestBuilder
import org.asynchttpclient.Response
import org.asynchttpclient.filter.FilterContext
import org.asynchttpclient.filter.ResponseFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AhcSupportTest {

    @Test
    fun `requestFilter handler returns same context and mutates request`() {
        val filter = requestFilter { ctx ->
            ctx.request.headers.add("x-app-key", "app-123")
        }
        val context = newFilterContext()

        val filtered = filter.filter(context)

        assertSame(context, filtered)
        assertEquals("app-123", filtered.request.headers.get("x-app-key"))
    }

    @Test
    fun `requestFilter builder can rebuild context`() {
        val filter = requestFilter(builder = {
            replayRequest(true)
        })
        val context = newFilterContext()

        val filtered = filter.filter(context)

        assertFalse(context.replayRequest())
        assertTrue(filtered.replayRequest())
    }

    @Test
    fun `AttachHeaderRequestFilter adds static headers`() {
        val filter = AttachHeaderRequestFilter(
            mapOf(
                "x-app-key" to "app-123",
                "x-user-id" to 42,
            )
        )
        val context = newFilterContext()

        val filtered = filter.filter(context)

        assertEquals("app-123", filtered.request.headers.get("x-app-key"))
        assertEquals("42", filtered.request.headers.get("x-user-id"))
    }

    @Test
    fun `DynamicAttachHandlerRequest keeps successful headers when one header resolution fails`() {
        val filter = DynamicAttachHandlerRequest(listOf("a", "broken", "b")) { name ->
            when (name) {
                "a" -> "value-a"
                "b" -> "value-b"
                else -> error("boom")
            }
        }
        val context = newFilterContext()

        val filtered = filter.filter(context)

        assertEquals("value-a", filtered.request.headers.get("a"))
        assertEquals("value-b", filtered.request.headers.get("b"))
        assertFalse(filtered.request.headers.contains("broken"))
    }

    @Test
    fun `attachHeaderRequestFilterOf appends static and dynamic headers`() {
        val staticFilter = attachHeaderRequestFilterOf(mapOf("x-static" to "1"))
        val dynamicFilter = attachHeaderRequestFilterOf(
            namesSupplier = { listOf("x-a", "x-b") },
            valueSupplier = { key -> "$key-value" }
        )
        val context = newFilterContext()

        staticFilter.filter(context)
        dynamicFilter.filter(context)

        assertEquals("1", context.request.headers.get("x-static"))
        assertEquals("x-a-value", context.request.headers.get("x-a"))
        assertEquals("x-b-value", context.request.headers.get("x-b"))
    }

    @Test
    fun `asyncHttpClientConfigOf registers filters and preserves default options`() {
        val requestFilter = requestFilter(handler = { _: FilterContext<*> -> })
        val responseFilter = object: ResponseFilter {
            override fun <T> filter(ctx: FilterContext<T>): FilterContext<T> = ctx
        }

        val config = asyncHttpClientConfigOf(
            requestFilters = listOf(requestFilter),
            responseFilters = listOf(responseFilter),
        )

        assertTrue(config.isCompressionEnforced)
        assertTrue(config.isFollowRedirect)
        assertTrue(config.isKeepAlive)
        assertEquals(5, config.maxRedirects)
        assertEquals(3, config.maxRequestRetry)
        assertEquals(1, config.requestFilters.size)
        assertEquals(1, config.responseFilters.size)
    }

    @Test
    fun `asyncHttpClient applies default config and custom overrides`() {
        val client = asyncHttpClient {
            setMaxRequestRetry(9)
        }

        try {
            val config = (client as org.asynchttpclient.DefaultAsyncHttpClient).config
            assertTrue(config.isCompressionEnforced)
            assertTrue(config.isFollowRedirect)
            assertTrue(config.isKeepAlive)
            assertEquals(9, config.maxRequestRetry)
        } finally {
            client.close()
        }
    }

    @Test
    fun `executeSuspending cancels underlying future when coroutine is cancelled`() = runTest {
        val cancelled = AtomicBoolean(false)
        val future = cancellableTestFuture(cancelled)
        val client = fakeAsyncHttpClient(future)
        val builder = BoundRequestBuilder(client, "GET", false).setUrl("http://localhost")

        val job: Job = launch(start = CoroutineStart.UNDISPATCHED) {
            builder.executeSuspending()
        }
        job.cancel()
        job.join()

        assertTrue(cancelled.get())
    }

    private fun newFilterContext(): FilterContext<Any> {
        val request = RequestBuilder("GET")
            .setUrl("https://example.com")
            .build()

        return FilterContext.FilterContextBuilder<Any>()
            .request(request)
            .build()
    }

    private fun cancellableTestFuture(cancelled: AtomicBoolean): ListenableFuture<Response> =
        object: ListenableFuture<Response> {
            private val completable = CompletableFuture<Response>()

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                cancelled.set(true)
                return completable.cancel(mayInterruptIfRunning)
            }

            override fun isCancelled(): Boolean = completable.isCancelled

            override fun isDone(): Boolean = completable.isDone

            override fun get(): Response = completable.get()

            override fun get(timeout: Long, unit: TimeUnit): Response = completable.get(timeout, unit)

            override fun done() = Unit

            override fun abort(t: Throwable) {
                completable.completeExceptionally(t)
            }

            override fun touch() = Unit

            override fun addListener(listener: Runnable, exec: Executor?): ListenableFuture<Response> {
                val action = Runnable { listener.run() }
                if (exec == null) {
                    completable.whenComplete { _, _ -> action.run() }
                } else {
                    completable.whenComplete { _, _ -> exec.execute(action) }
                }
                return this
            }

            override fun toCompletableFuture(): CompletableFuture<Response> = completable
        }

    @Suppress("UNCHECKED_CAST")
    private fun fakeAsyncHttpClient(future: ListenableFuture<Response>): AsyncHttpClient =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(AsyncHttpClient::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "executeRequest" -> future
                "isClosed"       -> false
                "close"          -> null
                "toString"       -> "FakeAsyncHttpClient"
                "hashCode"       -> System.identityHashCode(this)
                "equals"         -> false
                else             -> null
            }
        } as AsyncHttpClient
}
