package io.bluetape4k.http.okhttp3

import io.bluetape4k.concurrent.virtualthread.virtualThreadFactory
import io.bluetape4k.utils.Runtimex
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
 * Virtual Thread 기반 [okhttp3.Dispatcher]를 생성합니다.
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
 * 지정한 ExecutorService 기반 [okhttp3.Dispatcher]를 생성합니다.
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
        headers(okhttp3.Headers.headersOf(*nameAndValues))
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
