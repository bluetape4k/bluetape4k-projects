package io.bluetape4k.retrofit2.clients.vertx

import io.bluetape4k.http.vertx.defaultVertxHttpClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.okio.toTimeout
import io.bluetape4k.retrofit2.toIOException
import io.vertx.core.http.HttpClient
import io.vertx.kotlin.core.http.requestOptionsOf
import kotlinx.atomicfu.atomic
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Vert.x [HttpClient] 기반 Retrofit용 OkHttp `Call.Factory`를 생성합니다.
 *
 * ## 동작/계약
 * - [client]를 래핑한 [VertxCallFactory]를 반환합니다.
 * - [client] 기본값은 [defaultVertxHttpClient]입니다.
 *
 * ```kotlin
 * val callFactory = vertxCallFactoryOf()
 * // callFactory != null
 * ```
 */
fun vertxCallFactoryOf(client: HttpClient = defaultVertxHttpClient): VertxCallFactory {
    return VertxCallFactory(client)
}

/**
 * Vert.x HTTP 요청을 OkHttp [okhttp3.Call.Factory] 인터페이스로 어댑트한 구현입니다.
 *
 * ## 동작/계약
 * - [newCall]은 요청마다 독립 Call 인스턴스를 생성합니다.
 * - `execute()`는 내부 async 처리 결과를 timeout까지 대기하는 blocking 호출입니다.
 * - 네트워크/변환 오류는 [io.bluetape4k.retrofit2.toIOException]으로 변환됩니다.
 *
 * ```kotlin
 * val retrofit = retrofitOf(baseUrl, vertxCallFactoryOf())
 * // retrofit.callFactory()가 Vert.x 기반으로 동작
 * ```
 */
class VertxCallFactory private constructor(
    private val client: HttpClient,
): okhttp3.Call.Factory, java.io.Closeable {

    companion object: KLogging() {
        /** 기본 호출 타임아웃입니다. */
        val callTimeout: Duration = Duration.ofSeconds(30L)

        /**
         * [VertxCallFactory] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 [client] 인스턴스를 그대로 사용합니다.
         *
         * ```kotlin
         * val factory = VertxCallFactory(defaultVertxHttpClient)
         * // factory != null
         * ```
         */
        @JvmStatic
        operator fun invoke(client: HttpClient): VertxCallFactory {
            return VertxCallFactory(client)
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
        return VertxCall(request)
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
    ): okhttp3.Call {

        private val promiseRef = atomic<CompletableFuture<okhttp3.Response>?>(null)
        private var promise by promiseRef
        private val timeout = callTimeout.toTimeout()

        override fun execute(): okhttp3.Response {
            log.debug { "Execute VertxCall. request=$okRequest" }

            return try {
                executeAsync().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: ExecutionException) {
                throw (e.cause ?: e).toIOException()
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

            val options = requestOptionsOf(
                absoluteURI = okRequest.url.toString(),
                followRedirects = true,
                timeout = callTimeout.toMillis()
            )

            client.request(options)
                .onSuccess { clientRequest ->
                    val vertxRequest = okRequest.toVertxHttpClientRequest(clientRequest)
                    log.trace { "Send vertx request ... request=$vertxRequest, version=${vertxRequest.version()}" }

                    vertxRequest.send()
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
            promise?.let { promise ->
                if (!promise.cancel(true)) {
                    log.warn { "Cannot cancel promise. $promise" }
                }
            }
        }

        override fun isCanceled(): Boolean {
            return promise?.isCancelled ?: false
        }

        override fun clone(): okhttp3.Call {
            return VertxCall(okRequest)
        }

        override fun request(): okhttp3.Request {
            return okRequest
        }

        override fun timeout(): okio.Timeout {
            return timeout
        }

        override fun <T: Any> tag(type: KClass<T>): T? = null

        override fun <T> tag(type: Class<out T>): T? = null

        override fun <T: Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        override fun <T: Any> tag(type: Class<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        private fun throwAlreadyExecuted() {
            error("Already executed. request=$okRequest")
        }
    }
}
