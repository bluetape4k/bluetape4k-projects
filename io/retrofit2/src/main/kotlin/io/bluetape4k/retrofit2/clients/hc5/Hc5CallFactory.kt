package io.bluetape4k.retrofit2.clients.hc5

import io.bluetape4k.http.hc5.async.httpAsyncClientSystemOf
import io.bluetape4k.io.okio.toTimeout
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Apache HC5 비동기 클라이언트를 사용하는 Retrofit용 OkHttp `Call.Factory`를 생성합니다.
 *
 * ## 동작/계약
 * - [asyncClient]를 래핑한 [Hc5CallFactory]를 반환합니다.
 * - [asyncClient] 기본값은 시스템 설정 기반 `httpAsyncClientSystemOf()`입니다.
 *
 * ```kotlin
 * val callFactory = hc5CallFactoryOf()
 * // callFactory != null
 * ```
 */
fun hc5CallFactoryOf(
    asyncClient: CloseableHttpAsyncClient = httpAsyncClientSystemOf(),
): Hc5CallFactory {
    return Hc5CallFactory(asyncClient)
}

/**
 * Apache HC5 비동기 클라이언트를 OkHttp [okhttp3.Call.Factory]로 어댑트합니다.
 *
 * ## 동작/계약
 * - 생성 시 [asyncClient] 상태가 비활성(`!= ACTIVE`)이면 `start()`를 호출합니다.
 * - [newCall]은 요청마다 독립 Call 인스턴스를 생성합니다.
 * - `execute()`는 내부 async 처리 결과를 timeout까지 대기하는 blocking 호출입니다.
 *
 * ```kotlin
 * val retrofit = retrofitOf(baseUrl, hc5CallFactoryOf())
 * // retrofit.callFactory()가 HC5 기반으로 동작
 * ```
 */
class Hc5CallFactory private constructor(
    private val asyncClient: CloseableHttpAsyncClient,
): okhttp3.Call.Factory {

    companion object: KLogging() {
        /** 기본 호출 타임아웃입니다. */
        @JvmStatic
        val CallTimeout: Duration = Duration.ofSeconds(30)

        /**
         * [Hc5CallFactory] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 [asyncClient]를 그대로 사용합니다.
         *
         * ```kotlin
         * val factory = Hc5CallFactory(httpAsyncClientSystemOf())
         * // factory != null
         * ```
         */
        @JvmStatic
        operator fun invoke(asyncClient: CloseableHttpAsyncClient): Hc5CallFactory {
            return Hc5CallFactory(asyncClient)
        }
    }

    init {
        if (asyncClient.status != IOReactorStatus.ACTIVE) {
            asyncClient.start()
        }
    }

    override fun newCall(request: okhttp3.Request): okhttp3.Call {
        return AsyncClientCall(request)
    }

    private inner class AsyncClientCall(
        private val okRequest: okhttp3.Request,
        private val callTimeout: Duration = CallTimeout,
    ): okhttp3.Call {

        private val promiseRef = atomic<CompletableFuture<okhttp3.Response>?>(null)
        private var promise by promiseRef
        private val timeout = callTimeout.toTimeout()

        override fun execute(): okhttp3.Response {
            log.debug { "Execute Hc5Call. request=$okRequest" }

            return try {
                executeAsync().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: ExecutionException) {
                throw (e.cause ?: e).toIOException()
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

            asyncClient.execute(simpleRequest, object: FutureCallback<SimpleHttpResponse> {
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

            return promise
        }

        override fun isExecuted(): Boolean {
            return promise?.isDone ?: false
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
            return AsyncClientCall(okRequest)
        }

        override fun request(): okhttp3.Request {
            return okRequest
        }

        override fun timeout(): Timeout {
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
