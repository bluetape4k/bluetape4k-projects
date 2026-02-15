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
 * Apache HttpClient 5.x 를 사용하는 OkHttp3용 Call.Factory 구현체입니다.
 *
 * ```
 * val asyncClient = httpAsyncClientSystemOf()
 * val callFactory = hc5CallFactoryOf(asyncClient)
 *
 * val retrofit2 = retroift2Of("https://api.example.com", callFactory) {
 *    // ...
 * }
 * ```
 *
 * @param asyncClient Apache HttpClient 5.x의 CloseableHttpAsyncClient 인스턴스
 * @return OkHttp3의 Call.Factory 구현체
 */
fun hc5CallFactoryOf(
    asyncClient: CloseableHttpAsyncClient = httpAsyncClientSystemOf(),
): Hc5CallFactory {
    return Hc5CallFactory(asyncClient)
}

/**
 * Retrofit2 연동에서 사용하는 `Hc5CallFactory` 타입입니다.
 */
class Hc5CallFactory private constructor(
    private val asyncClient: CloseableHttpAsyncClient,
): okhttp3.Call.Factory {

    companion object: KLogging() {
        /** 기본 호출 타임아웃입니다. */
        @JvmStatic
        val CallTimeout: Duration = Duration.ofSeconds(30)

        /**
         * Retrofit2 연동용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(asyncClient: CloseableHttpAsyncClient): Hc5CallFactory {
            return Hc5CallFactory(asyncClient)
        }
    }

    init {
        // NOTE: 먼저 start() 를 호출해주어야 합니다.
        if (asyncClient.status != IOReactorStatus.ACTIVE) {
            asyncClient.start()
        }
    }

    /**
     * Retrofit2 연동에서 `newCall` 함수를 제공합니다.
     */
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

        /**
         * Retrofit2 연동에서 `execute` 함수를 제공합니다.
         */
        override fun execute(): okhttp3.Response {
            log.debug { "Execute Hc5Call. request=$okRequest" }

            return try {
                // execute 는 Async 이지만 Blocking 입니다.
                executeAsync().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch (e: ExecutionException) {
                throw (e.cause ?: e).toIOException()
            } catch (e: Throwable) {
                throw e.toIOException()
            }
        }

        /**
         * Retrofit2 연동에서 `enqueue` 함수를 제공합니다.
         */
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
                /**
                 * Retrofit2 연동에서 `completed` 함수를 제공합니다.
                 */
                override fun completed(result: SimpleHttpResponse) {
                    try {
                        val okResponse: okhttp3.Response = result.toOkHttp3Response(okRequest)
                        promise.complete(okResponse)
                    } catch (e: Exception) {
                        promise.completeExceptionally(e.toIOException())
                    }
                }

                /**
                 * Retrofit2 연동에서 `failed` 함수를 제공합니다.
                 */
                override fun failed(ex: java.lang.Exception) {
                    promise.completeExceptionally(IOException("Fail to execute. request=$okRequest", ex))
                }

                /**
                 * Retrofit2 연동에서 `cancelled` 함수를 제공합니다.
                 */
                override fun cancelled() {
                    promise.completeExceptionally(IOException("Cancelled. request=$okRequest"))
                }
            })

            return promise
        }

        /**
         * Retrofit2 연동에서 `isExecuted` 함수를 제공합니다.
         */
        override fun isExecuted(): Boolean {
            return promise?.isDone ?: false
        }

        /**
         * Retrofit2 연동에서 `cancel` 함수를 제공합니다.
         */
        override fun cancel() {
            promise?.let { promise ->
                if (!promise.cancel(true)) {
                    log.warn { "Cannot cancel promise. $promise" }
                }
            }
        }

        /**
         * Retrofit2 연동에서 `isCanceled` 함수를 제공합니다.
         */
        override fun isCanceled(): Boolean {
            return promise?.isCancelled ?: false
        }

        /**
         * Retrofit2 연동에서 `clone` 함수를 제공합니다.
         */
        override fun clone(): okhttp3.Call {
            return AsyncClientCall(okRequest)
        }

        /**
         * Retrofit2 연동에서 `request` 함수를 제공합니다.
         */
        override fun request(): okhttp3.Request {
            return okRequest
        }

        /**
         * Retrofit2 연동에서 `timeout` 함수를 제공합니다.
         */
        override fun timeout(): Timeout {
            return timeout
        }

        /**
         * Retrofit2 연동에서 `tag` 함수를 제공합니다.
         */
        override fun <T: Any> tag(type: KClass<T>): T? = null

        /**
         * Retrofit2 연동에서 `tag` 함수를 제공합니다.
         */
        override fun <T> tag(type: Class<out T>): T? = null

        /**
         * Retrofit2 연동에서 `tag` 함수를 제공합니다.
         */
        override fun <T: Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        /**
         * Retrofit2 연동에서 `tag` 함수를 제공합니다.
         */
        override fun <T: Any> tag(type: Class<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        private fun throwAlreadyExecuted() {
            error("Already executed. request=$okRequest")
        }
    }
}
