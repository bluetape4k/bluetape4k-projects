package io.bluetape4k.retrofit2.clients.vertx

import io.bluetape4k.http.vertx.defaultVertxHttpClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.okio.toTimeout
import io.bluetape4k.retrofit2.toIOException
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientRequest
import io.vertx.kotlin.core.http.requestOptionsOf
import kotlinx.atomicfu.atomic
import okhttp3.EventListener
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

/**
 * Creates a Retrofit-compatible OkHttp `Call.Factory` backed by a Vert.x [HttpClient].
 *
 * ## Contract
 * - Wraps [client] without taking ownership beyond [VertxCallFactory.close].
 * - Uses [callTimeout] for blocking `execute()` waits, Vert.x request timeout, and the advertised Okio timeout.
 * - A blocking timeout or interruption resets the underlying Vert.x request when one exists.
 *
 * ```kotlin
 * val callFactory = vertxCallFactoryOf(callTimeout = Duration.ofSeconds(10))
 * // callFactory can be passed to Retrofit.Builder.callFactory(...)
 * ```
 */
fun vertxCallFactoryOf(
    client: HttpClient = defaultVertxHttpClient,
    callTimeout: Duration = VertxCallFactory.callTimeout,
): VertxCallFactory {
    return VertxCallFactory(client, callTimeout)
}

/**
 * Adapts Vert.x HTTP requests to OkHttp [okhttp3.Call.Factory].
 *
 * ## Contract
 * - [newCall] creates an independent call instance per request.
 * - `execute()` blocks up to [callTimeout], resets the Vert.x request on timeout/interruption,
 *   and restores the thread interrupt flag for interruptions.
 * - Network and conversion failures are normalized with [io.bluetape4k.retrofit2.toIOException].
 *
 * ```kotlin
 * val retrofit = retrofitOf(baseUrl, vertxCallFactoryOf(callTimeout = Duration.ofSeconds(10)))
 * // retrofit.callFactory() uses the Vert.x transport adapter
 * ```
 */
class VertxCallFactory private constructor(
    private val client: HttpClient,
    private val defaultCallTimeout: Duration,
): okhttp3.Call.Factory, java.io.Closeable {

    companion object: KLogging() {
        /** Default call timeout. */
        val callTimeout: Duration = Duration.ofSeconds(30L)

        /**
         * Creates a [VertxCallFactory] from an existing Vert.x [HttpClient].
         *
         * ## Contract
         * - Keeps using the caller-provided [client].
         * - Applies [callTimeout] to each new call.
         *
         * ```kotlin
         * val factory = VertxCallFactory(defaultVertxHttpClient, Duration.ofSeconds(10))
         * // factory != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            client: HttpClient,
            callTimeout: Duration = VertxCallFactory.callTimeout,
        ): VertxCallFactory {
            return VertxCallFactory(client, callTimeout)
        }
    }

    /**
     * 새 [okhttp3.Call] 인스턴스를 생성합니다.
     *
     * ```kotlin
     * val factory = vertxCallFactoryOf()
     * val request = okhttp3.Request.Builder().url("https://example.com").build()
     * val call = factory.newCall(request)
     * // call != null
     * ```
     */
    override fun newCall(request: okhttp3.Request): okhttp3.Call {
        return VertxCall(request, defaultCallTimeout)
    }

    /**
     * 내부 Vert.x HTTP 클라이언트를 종료합니다.
     *
     * ```kotlin
     * val factory = vertxCallFactoryOf()
     * factory.close()
     * // 내부 Vert.x HttpClient 종료됨
     * ```
     */
    override fun close() {
        client.close()
    }

    private inner class VertxCall(
        private val okRequest: okhttp3.Request,
        private val callTimeout: Duration,
    ): okhttp3.Call {

        private val promiseRef = atomic<CompletableFuture<okhttp3.Response>?>(null)
        private var promise by promiseRef
        private val timeout = callTimeout.toTimeout()
        private val tags = ConcurrentHashMap<Class<*>, Any>()
        private val eventListeners = CopyOnWriteArrayList<EventListener>()
        private val cancelledRef = atomic(false)
        private var cancelled by cancelledRef

        @Volatile
        private var vertxRequest: HttpClientRequest? = null

        override fun execute(): okhttp3.Response {
            log.debug { "Execute VertxCall. request=$okRequest" }

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
            log.debug { "Enqueue VertxCall. request=$okRequest" }

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
            eventListeners.forEach { it.callStart(this) }
            promise.whenComplete { _, error ->
                if (error == null) {
                    eventListeners.forEach { it.callEnd(this) }
                } else {
                    val failure = error.toIOException()
                    eventListeners.forEach { it.callFailed(this, failure) }
                }
            }

            val options = requestOptionsOf(
                absoluteURI = okRequest.url.toString(),
                followRedirects = true,
                timeout = callTimeout.toMillis()
            )

            client.request(options)
                .onSuccess { clientRequest ->
                    val req = okRequest.toVertxHttpClientRequest(clientRequest)
                    vertxRequest = req
                    log.trace { "Send vertx request ... request=$req, version=${req.version()}" }

                    // If cancel() was called before vertxRequest was assigned, reset and
                    // complete the promise so enqueue() callbacks fire and execute() unblocks.
                    if (cancelled) {
                        req.reset()
                        promise.cancel(true)
                        return@onSuccess
                    }

                    req.send()
                        .onSuccess { vertxResponse ->
                            vertxResponse.toOkResponse(okRequest, promise)
                        }
                        .onFailure { error ->
                            promise.completeExceptionally(error.toIOException())
                        }
                }
                .onFailure { error ->
                    promise.completeExceptionally(error.toIOException())
                }

            return promise
        }

        override fun isExecuted(): Boolean {
            return promise != null
        }

        override fun cancel() {
            if (!cancelledRef.compareAndSet(false, true)) {
                return
            }
            eventListeners.forEach { it.canceled(this) }
            promise?.cancel(true)
            // reset() is idempotent in Vert.x 5.x (returns false on subsequent calls),
            // so a concurrent double-reset between executeAsync and cancel() is safe.
            vertxRequest?.reset()
        }

        override fun isCanceled(): Boolean {
            return cancelled
        }

        override fun clone(): okhttp3.Call {
            return VertxCall(okRequest, callTimeout)
        }

        override fun request(): okhttp3.Request {
            return okRequest
        }

        override fun timeout(): okio.Timeout {
            return timeout
        }

        override fun addEventListener(eventListener: EventListener) {
            eventListeners += eventListener
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
