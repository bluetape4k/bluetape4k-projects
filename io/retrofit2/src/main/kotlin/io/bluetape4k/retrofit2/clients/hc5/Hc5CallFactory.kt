package io.bluetape4k.retrofit2.clients.hc5

import io.bluetape4k.http.hc5.async.httpAsyncClientSystemOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.okio.toTimeout
import io.bluetape4k.retrofit2.toIOException
import kotlinx.atomicfu.atomic
import okio.Timeout
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.reactor.IOReactorStatus
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

/**
 * Creates a Retrofit-compatible OkHttp `Call.Factory` backed by Apache HC5 async client.
 *
 * ## Contract
 * - Wraps [asyncClient] without taking ownership beyond [Hc5CallFactory.close].
 * - Uses [callTimeout] for blocking `execute()` waits and the advertised Okio timeout.
 * - A blocking timeout or interruption cancels the underlying HC5 request.
 *
 * ```kotlin
 * val callFactory = hc5CallFactoryOf(callTimeout = Duration.ofSeconds(10))
 * // callFactory can be passed to Retrofit.Builder.callFactory(...)
 * ```
 */
fun hc5CallFactoryOf(
    asyncClient: CloseableHttpAsyncClient = httpAsyncClientSystemOf(),
    callTimeout: Duration = Hc5CallFactory.CallTimeout,
): Hc5CallFactory {
    return Hc5CallFactory(asyncClient, callTimeout)
}

/**
 * Adapts an Apache HC5 async client to OkHttp [okhttp3.Call.Factory].
 *
 * ## Contract
 * - Starts [asyncClient] when it is not already active.
 * - [newCall] creates an independent call instance per request.
 * - `execute()` blocks up to [callTimeout], cancels the HC5 future on timeout/interruption,
 *   and restores the thread interrupt flag for interruptions.
 *
 * ```kotlin
 * val retrofit = retrofitOf(baseUrl, hc5CallFactoryOf(callTimeout = Duration.ofSeconds(10)))
 * // retrofit.callFactory() uses the HC5 transport adapter
 * ```
 */
class Hc5CallFactory private constructor(
    private val asyncClient: CloseableHttpAsyncClient,
    private val callTimeout: Duration,
): okhttp3.Call.Factory, java.io.Closeable {

    companion object: KLogging() {
        /** 기본 호출 타임아웃입니다. */
        @JvmStatic
        val CallTimeout: Duration = Duration.ofSeconds(30)

        /**
         * Creates an [Hc5CallFactory] from an existing HC5 async client.
         *
         * ## Contract
         * - Keeps using the caller-provided [asyncClient].
         * - Applies [callTimeout] to each new call.
         *
         * ```kotlin
         * val factory = Hc5CallFactory(httpAsyncClientSystemOf(), Duration.ofSeconds(10))
         * // factory != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            asyncClient: CloseableHttpAsyncClient,
            callTimeout: Duration = CallTimeout,
        ): Hc5CallFactory {
            return Hc5CallFactory(asyncClient, callTimeout)
        }
    }

    init {
        if (asyncClient.status != IOReactorStatus.ACTIVE) {
            asyncClient.start()
        }
    }

    /**
     * 새 [okhttp3.Call] 인스턴스를 생성합니다.
     *
     * ```kotlin
     * val factory = hc5CallFactoryOf()
     * val request = okhttp3.Request.Builder().url("https://example.com").build()
     * val call = factory.newCall(request)
     * // call != null
     * ```
     */
    override fun newCall(request: okhttp3.Request): okhttp3.Call {
        return AsyncClientCall(request, callTimeout)
    }

    /**
     * 내부 HC5 비동기 클라이언트를 종료합니다.
     *
     * ```kotlin
     * val factory = hc5CallFactoryOf()
     * factory.close()
     * // 내부 asyncClient 종료됨
     * ```
     */
    override fun close() {
        asyncClient.close()
    }

    private inner class AsyncClientCall(
        private val okRequest: okhttp3.Request,
        private val callTimeout: Duration = CallTimeout,
    ): okhttp3.Call {

        private val promiseRef = atomic<CompletableFuture<okhttp3.Response>?>(null)
        private var promise by promiseRef
        private val timeout = callTimeout.toTimeout()
        private val tags = ConcurrentHashMap<Class<*>, Any>()

        @Volatile
        private var cancelled = false

        @Volatile
        private var hc5Future: Future<SimpleHttpResponse>? = null

        override fun execute(): okhttp3.Response {
            log.debug { "Execute Hc5Call. request=$okRequest" }

            return try {
                executeAsync().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: ExecutionException) {
                val cause = e.cause ?: e
                if (cause.isTimeoutLikeFailure()) {
                    cancel()
                }
                throw cause.toIOException()
            } catch (e: TimeoutException) {
                cancel()
                throw IOException("Call timed out after $callTimeout. request=$okRequest", e)
            } catch (e: InterruptedException) {
                cancel()
                Thread.currentThread().interrupt()
                throw IOException("Call interrupted. request=$okRequest", e)
            } catch (e: Throwable) {
                throw e.toIOException()
            }
        }

        override fun enqueue(responseCallback: okhttp3.Callback) {
            log.debug { "Enqueue Hc5Call. request=$okRequest" }

            executeAsync()
                .whenComplete { response, error ->
                    if (error != null) {
                        responseCallback.onFailure(this, error.toIOException())
                    } else {
                        responseCallback.onResponse(this, response)
                    }
                }
        }

        private fun executeAsync(): CompletableFuture<okhttp3.Response> {
            if (promise != null) {
                throwAlreadyExecuted()
            }
            val promise = CompletableFuture<okhttp3.Response>()
            if (!promiseRef.compareAndSet(null, promise)) {
                throwAlreadyExecuted()
            }

            val simpleRequest = okRequest.toSimpleHttpRequest()

            hc5Future = asyncClient.execute(simpleRequest, object: FutureCallback<SimpleHttpResponse> {
                override fun completed(result: SimpleHttpResponse) {
                    try {
                        val okResponse: okhttp3.Response = result.toOkHttp3Response(okRequest)
                        promise.complete(okResponse)
                    } catch (e: Exception) {
                        promise.completeExceptionally(e.toIOException())
                    }
                }

                override fun failed(ex: java.lang.Exception) {
                    promise.completeExceptionally(IOException("Fail to execute. request=$okRequest", ex))
                }

                override fun cancelled() {
                    promise.completeExceptionally(IOException("Cancelled. request=$okRequest"))
                }
            })

            // If cancel() was called before hc5Future was assigned, cancel now
            if (cancelled) {
                hc5Future?.cancel(true)
            }

            return promise
        }

        override fun isExecuted(): Boolean {
            return promise != null
        }

        override fun cancel() {
            cancelled = true
            promise?.cancel(true)
            hc5Future?.cancel(true)
        }

        override fun isCanceled(): Boolean {
            return cancelled
        }

        override fun clone(): okhttp3.Call {
            return AsyncClientCall(okRequest)
        }

        override fun request(): okhttp3.Request {
            return okRequest
        }

        override fun timeout(): Timeout {
            return timeout
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> tag(type: KClass<T>): T? =
            (tags[type.java] ?: okRequest.tag(type.java)) as? T

        @Suppress("UNCHECKED_CAST")
        override fun <T> tag(type: Class<out T>): T? =
            (tags[type] ?: okRequest.tag(type)) as? T

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T =
            tags.computeIfAbsent(type.java) { okRequest.tag(type.java) ?: computeIfAbsent() } as T

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> tag(type: Class<T>, computeIfAbsent: () -> T): T =
            tags.computeIfAbsent(type) { okRequest.tag(type) ?: computeIfAbsent() } as T

        private fun throwAlreadyExecuted() {
            error("Already executed. request=$okRequest")
        }

        private fun Throwable.isTimeoutLikeFailure(): Boolean =
            this is TimeoutException ||
                message?.contains("timeout", ignoreCase = true) == true ||
                message?.contains("timed out", ignoreCase = true) == true
    }
}
