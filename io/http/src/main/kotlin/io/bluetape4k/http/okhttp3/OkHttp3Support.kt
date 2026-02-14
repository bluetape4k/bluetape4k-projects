package io.bluetape4k.http.okhttp3

import io.bluetape4k.concurrent.virtualthread.virtualThreadFactory
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/**
 * [OkHttpClient]의 Connection Pool을 생성합니다.
 *
 * ```
 * val connectionPool = okHttp3ConnectionPoolOf(
 *      maxIdleConnections = 5
 *      keepAliveDurations = Duration.ofMinutes(5)
 * )
 * ```
 *
 * @param maxIdleConnections 최대 유휴 커넥션 수
 * @param keepAliveDurations 커넥션 유지 시간
 * @return [ConnectionPool] 인스턴스
 */
fun okHttp3ConnectionPool(
    maxIdleConnections: Int = Runtimex.availableProcessors,
    keepAliveDurations: Duration = Duration.ofMinutes(5),
): ConnectionPool =
    ConnectionPool(maxIdleConnections, keepAliveDurations.toSeconds(), TimeUnit.SECONDS)

/**
 * [OkHttpClient]의 Connection Pool을 생성합니다.
 *
 * ```
 * val connectionPool = okHttp3ConnectionPoolOf(
 *  maxIdleConnections = 5
 *  keepAliveDurations = Duration.ofMinutes(5)
 * )
 *
 * val clientBuilder = okhttp3ClientBuilderOf(connectionPool) {
 *    readTimeout(Duration.ofSeconds(10))
 *    writeTimeout(Duration.ofSeconds(30))
 * }
 *
 * val client = clientBuilder.build()
 * ```
 *
 * @param connectionPool [ConnectionPool] 인스턴스
 * @param builder [OkHttpClient.Builder] 초기화 람다
 * @return [OkHttpClient.Builder] 인스턴스
 */
inline fun okhttp3ClientBuilderOf(
    connectionPool: ConnectionPool = okHttp3ConnectionPool(),
    dispatcher: okhttp3.Dispatcher = okhttp3DispatcherWithVirtualThread(),
    @BuilderInference builder: OkHttpClient.Builder.() -> Unit = {},
): OkHttpClient.Builder =
    OkHttpClient.Builder()
        .apply {
            connectionPool(connectionPool)
            dispatcher(dispatcher)
            connectTimeout(Duration.ofSeconds(10))
            readTimeout(Duration.ofSeconds(10))
            writeTimeout(Duration.ofSeconds(30))

            builder()
        }

/**
 * HTTP 처리에서 `okhttp3DispatcherWithVirtualThread` 함수를 제공합니다.
 */
fun okhttp3DispatcherWithVirtualThread(
    threadName: String = "okhttp3-virtual-thread-",
): okhttp3.Dispatcher {
    val factory = virtualThreadFactory {
        name(threadName, 0)
        inheritInheritableThreadLocals(true)
    }
    val executor = Executors.newThreadPerTaskExecutor(factory)
    return okhttp3DispatcherOf(executor)
}

/**
 * HTTP 처리에서 `okhttp3DispatcherOf` 함수를 제공합니다.
 */
fun okhttp3DispatcherOf(
    executorService: java.util.concurrent.ExecutorService = Executors.newVirtualThreadPerTaskExecutor(),
): okhttp3.Dispatcher {
    return okhttp3.Dispatcher(executorService)
}

/**
 * [OkHttpClient]를 생성합니다.
 *
 * ```
 * val connectionPool = okHttp3ConnectionPoolOf(
 *  maxIdleConnections = 5
 *  keepAliveDurations = Duration.ofMinutes(5)
 * )
 *
 * val client = okhttp3Client(connectionPool) {
 *     addInterceptor(LoggingInterceptor(logger))
 *     addInterceptor(CachingResponseInterceptor())
 * }
 * ```
 *
 * @param connectionPool [ConnectionPool] 인스턴스
 * @param builder [OkHttpClient.Builder] 초기화 람다
 * @return [OkHttpClient] 인스턴스
 */
inline fun okhttp3Client(
    connectionPool: ConnectionPool = okHttp3ConnectionPool(),
    dispatcher: okhttp3.Dispatcher = okhttp3DispatcherWithVirtualThread(),
    @BuilderInference builder: OkHttpClient.Builder.() -> Unit = {},
): OkHttpClient =
    okhttp3ClientBuilderOf(connectionPool, dispatcher).apply(builder).build()

/**
 * OkHttp3의 [CacheControl]을 생성합니다.
 *
 * ```
 * val cacheControl = okhttp3CacheControl {
 *    maxAge(10, TimeUnit.SECONDS)
 *    maxStale(10, TimeUnit.SECONDS)
 *    minFresh(10, TimeUnit.SECONDS)
 *    onlyIfCached()
 * }
 *
 * val request = okhttp3Request {
 *   url("https://example.com")
 *   cacheControl(cacheControl)
 * }
 * ```
 *
 * @param builder [CacheControl.Builder] 초기화 람다
 * @return [CacheControl] 인스턴스
 */
inline fun okhttp3CacheControl(
    @BuilderInference builder: CacheControl.Builder.() -> Unit,
): CacheControl =
    CacheControl.Builder().apply(builder).build()

/**
 * OkHttp3의 [CacheControl]을 생성합니다.
 *
 * ```
 * val cacheControl = okhttp3CacheControlOf(
 *    maxAgeInSeconds = 10,
 *    maxStaleInSeconds = 10,
 *    minFreshInSeconds = 10,
 *    onlyIfCached = true
 * )
 *
 * val request = okhttp3Request {
 *   url("https://example.com")
 *   cacheControl(cacheControl)
 * }
 * ```
 *
 * @param maxAgeInSeconds 최대 유효 시간
 * @param maxStaleInSeconds 최대 유효 시간
 * @param minFreshInSeconds 최소 유효 시간
 * @param onlyIfCached 캐시만 사용
 * @param noCache 캐시 사용 금지
 * @param noStore 캐시 저장 금지
 * @param noTransform 캐시 변환 금지
 * @param immutable 캐시 불변
 * @return [CacheControl] 인스턴스
 */
inline fun okhttp3CacheControlOf(
    maxAgeInSeconds: Int = 0,
    maxStaleInSeconds: Int = 0,
    minFreshInSeconds: Int = 0,
    onlyIfCached: Boolean = false,
    noCache: Boolean = false,
    noStore: Boolean = false,
    noTransform: Boolean = false,
    immutable: Boolean = false,
    @BuilderInference builder: CacheControl.Builder.() -> Unit = {},
): CacheControl =
    okhttp3CacheControl {
        maxAge(maxAgeInSeconds, TimeUnit.SECONDS)
        maxStale(maxStaleInSeconds, TimeUnit.SECONDS)
        minFresh(minFreshInSeconds, TimeUnit.SECONDS)
        if (onlyIfCached) onlyIfCached()
        if (noCache) noCache()
        if (noStore) noStore()
        if (noTransform) noTransform()
        if (immutable) immutable()

        builder()
    }

/**
 * OkHttp3의 [okhttp3.Request]를 생성합니다.
 *
 * ```
 * val request = okhttp3Request {
 *   url("https://example.com")
 *   headers {
 *     add("Accept", "application/json")
 *   }
 * }
 * ```
 *
 * @param builder [okhttp3.Request.Builder] 초기화 람다
 * @return [okhttp3.Request] 인스턴스
 */
inline fun okhttp3Request(
    @BuilderInference builder: okhttp3.Request.Builder.() -> Unit,
): okhttp3.Request =
    okhttp3.Request.Builder().apply(builder).build()

/**
 * OkHttp3의 [okhttp3.Request]를 생성합니다.
 *
 * ```
 * val request = okhttp3RequestOf("https://example.com") {
 *     get()
 *     cacheControl(cacheControl)
 *     tag("tag")
 * }
 * ```
 *
 * @param url 요청 URL
 * @param nameAndValues 헤더 정보
 * @param builder [okhttp3.Request.Builder] 초기화 람다
 * @return [okhttp3.Request] 인스턴스
 */
inline fun okhttp3RequestOf(
    url: String,
    vararg nameAndValues: String,
    @BuilderInference builder: okhttp3.Request.Builder.() -> Unit = {},
): okhttp3.Request =
    okhttp3Request {
        url(url)
        okhttp3.Headers.headersOf(*nameAndValues)
        builder()
    }

/**
 * OkHttp3의 [okhttp3.Request]를 생성합니다.
 *
 * ```
 * val request = okhttp3RequestOf("https://example.com", headers) {
 *     get()
 *     cacheControl(cacheControl)
 *     tag("tag")
 * }
 * ```
 *
 * @param url 요청 URL
 * @param headers 헤더 정보
 * @param builder [okhttp3.Request.Builder] 초기화 람다
 * @return [okhttp3.Request] 인스턴스
 */
inline fun okhttp3RequestOf(
    url: String,
    headers: okhttp3.Headers,
    @BuilderInference builder: okhttp3.Request.Builder.() -> Unit = {},
): okhttp3.Request =
    okhttp3Request {
        url(url)
        headers(headers)
        builder()
    }

/**
 * OkHttp3의 [okhttp3.Response]를 생성합니다.
 *
 * ```
 * val response = okhttp3Response {
 *   code(200)
 *   message("OK")
 *   headers {
 *     add("Content-Type", "application/json")
 *   }
 *   body(okhttp3ResponseBody {
 *     string("Hello, World!")
 *   })
 * }
 * ```
 *
 * @param builder [okhttp3.Response.Builder] 초기화 람다
 * @return [okhttp3.Response] 인스턴스
 */
inline fun okhttp3Response(
    @BuilderInference builder: okhttp3.Response.Builder.() -> Unit,
): okhttp3.Response =
    okhttp3.Response.Builder().apply(builder).build()

/**
 * OkHttp3의 Response Body를 InputStream으로 변환합니다.
 *
 * ```
 * val response = client.execute(request)
 * val inputStream = response.bodyAsInputStream()
 * ```
 *
 * @receiver [okhttp3.Response] 인스턴스
 * @return [InputStream] 인스턴스
 */
fun okhttp3.Response?.bodyAsInputStream(): InputStream? = this?.body?.byteStream()

/**
 * OkHttp3의 Response Body를 ByteArray로 변환합니다.
 *
 * ```
 * val response = client.execute(request)
 * val byteArray = response.bodyAsByteArray()
 * ```
 *
 * @receiver [okhttp3.Response] 인스턴스
 * @return [ByteArray] 인스턴스
 */
fun okhttp3.Response?.bodyAsByteArray(): ByteArray? = this?.body?.bytes()

/**
 * OkHttp3의 Response Body를 String으로 변환합니다.
 *
 * ```
 * val response = client.execute(request)
 * val string = response.bodyAsString()
 * ```
 *
 * @receiver [okhttp3.Response] 인스턴스
 * @return [String] 인스턴스
 */
fun okhttp3.Response?.bodyAsString(): String? = this?.body?.string()

/**
 * [request]를 전송하고, [okhttp3.Response]를 반환합니다.
 *
 * ```
 * val response = client.execute(request)
 * ```
 *
 * @param request [okhttp3.Request] 인스턴스
 * @receiver [OkHttpClient] 인스턴스
 */
fun OkHttpClient.execute(request: okhttp3.Request): okhttp3.Response = newCall(request).execute()

/**
 * [OkHttpClient]를 비동기 방식으로 실행합니다. (단 CompletableFuture를 반환하므로, Non-Blocking 은 아닙니다)
 *
 * ```
 * val responseFuture = client.executeAsync(request)
 * val body = responseFuture.get().bodyAsString()
 * ```
 *
 * @param request [okhttp3.Request] 인스턴스
 * @param cancelHandler 취소된 경우에 호출할 handler
 * @receiver [OkHttpClient] 인스턴스
 * @return [okhttp3.Response]를 가지는 CompletableFuture 인스턴스
 */
inline fun OkHttpClient.executeAsync(
    request: okhttp3.Request,
    crossinline cancelHandler: (Throwable) -> Unit = {},
): CompletableFuture<okhttp3.Response> {
    val promise = CompletableFuture<okhttp3.Response>()

    val callback = object: Callback {
        /**
         * HTTP 처리에서 `onResponse` 함수를 제공합니다.
         */
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            when {
                response.isSuccessful -> promise.complete(response)
                call.isCanceled() -> handleCanceled(IOException("Canceled"))
                else -> handleCanceled(IOException("Unexpected code $response"))
            }
        }

        /**
         * HTTP 처리에서 `onFailure` 함수를 제공합니다.
         */
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            if (call.isCanceled()) {
                handleCanceled(e)
            } else {
                promise.completeExceptionally(e)
            }
        }

        private fun handleCanceled(e: IOException) {
            cancelHandler(e)
            promise.completeExceptionally(e)
        }
    }

    newCall(request).enqueue(callback)
    return promise
}

/**
 * Coroutines 환경에서 [request]를 전송하고, [okhttp3.Response]를 반환합니다.
 *
 * ```
 * runBlocking {
 *     val response = client.executeSuspending(request)
 * }
 * ```
 *
 * @param request [okhttp3.Request] 인스턴스
 * @receiver [OkHttpClient] 인스턴스
 */
suspend inline fun OkHttpClient.suspendExecute(request: okhttp3.Request): Response =
    newCall(request).suspendExecute()

/**
 * [Call]을 Coroutines 방식으로 실행합니다. (Non-Blocking 방식입니다)
 *
 * ```
 * runBlocking {
 *      val response = call.executeSuspending()
 * }
 * ```
 *
 * @receiver [Call] 인스턴스
 */
suspend inline fun Call.suspendExecute(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {
        this.cancel()
    }

    val responseCallback = object: Callback {
        /**
         * HTTP 처리에서 `onResponse` 함수를 제공합니다.
         */
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { cause, _, _ -> call.cancel() }
        }

        /**
         * HTTP 처리에서 `onFailure` 함수를 제공합니다.
         */
        override fun onFailure(call: Call, e: IOException) {
            if (call.isCanceled()) {
                cont.cancel(e)
            } else {
                cont.resumeWithException(e)
            }
        }
    }
    enqueue(responseCallback)
}

/**
 * [okhttp3.Response]를 출력합니다.
 *
 * ```
 * val response = client.execute(request)
 * response.print()
 * ```
 *
 * @receiver [okhttp3.Response] 인스턴스
 * @param no 출력 번호
 */
fun okhttp3.Response.print(no: Int = 1) {
    println("Response[$no]: ${this.code} ${this.message}")
    println("Headers[$no]: ${this.headers}")
    println("Cache Response[$no]: ${this.cacheResponse}")
    println("Network Response[$no]: ${this.networkResponse}")
}

/**
 * [okhttp3.MediaType] 정보를 문자열로 변환합니다.
 *
 * ```
 * val mediaType = response.body?.contentType()
 * println(mediaType.toTypeString())
 * ```
 *
 * @receiver [okhttp3.MediaType] 인스턴스
 */
fun okhttp3.MediaType.toTypeString(): String = "${this.type}/${this.subtype}"
