package io.bluetape4k.http.benchmark

import com.github.tomakehurst.wiremock.client.WireMock
import io.bluetape4k.http.ahc.asyncHttpClient
import io.bluetape4k.http.ahc.executeSuspending
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf
import io.bluetape4k.http.okhttp3.okhttp3DispatcherWithVirtualThread
import io.bluetape4k.testcontainers.http.WireMockServer
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.benchmark.Warmup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher as OkHttpDispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.openjdk.jmh.annotations.Threads
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 고지연(40~100 ms) 환경에서 동기 vs 비동기 HTTP 클라이언트 처리량 비교.
 *
 * WireMock Docker 서버의 `withFixedDelay(ms)` stub을 사용해
 * 서버측 지연을 정밀하게 시뮬레이션합니다.
 *
 * 핵심 가설: 지연이 커질수록 동기 블로킹 클라이언트는 스레드를 점유해
 * 처리량이 제한되고, 비동기 + 코루틴 클라이언트가 역전을 일으킬 수 있다.
 *
 * 측정 조건:
 * - 서버 응답 지연: [DELAY_MS] ms (WireMock fixed delay)
 * - JMH 스레드: [THREAD_COUNT]
 * - 예상 동기 상한: THREAD_COUNT × (1000 / DELAY_MS) ops/s
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(50)
open class HttpClientLatencyBenchmark {

    companion object {
        /** 서버 응답 지연 (밀리초). 40~100 사이에서 조정 */
        const val DELAY_MS = 50
        /** JMH 스레드 수. 동기 클라이언트의 이론적 상한 = 50 × (1000/DELAY_MS) = 1000 ops/s */
        const val THREAD_COUNT = 50
    }

    private val wireMock = WireMockServer.Launcher.wireMock

    private lateinit var delayUrl: String
    private lateinit var serverHost: String
    private var serverPort: Int = 0

    private lateinit var okhttpClient: OkHttpClient
    private lateinit var okhttpVtClient: OkHttpClient
    private lateinit var jdkClient: HttpClient
    private lateinit var jdkVirtualClient: HttpClient
    private lateinit var hc5Classic: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5ClassicVt: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5Async: org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
    private lateinit var ahcOptimizedClient: org.asynchttpclient.AsyncHttpClient
    private lateinit var vertx: Vertx
    private lateinit var vertxWebClient: WebClient

    @Setup
    fun setup() {
        val n = Runtime.getRuntime().availableProcessors()

        // 고정 지연 stub 등록: GET /delayed → 200 after DELAY_MS ms
        wireMock.stubFor(
            WireMock.get("/delayed")
                .willReturn(
                    WireMock.ok("pong")
                        .withFixedDelay(DELAY_MS)
                )
        )

        serverHost = wireMock.host
        serverPort = wireMock.port
        delayUrl = "${wireMock.url}/delayed"

        okhttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(THREAD_COUNT * 2, 5L, TimeUnit.MINUTES))
            .dispatcher(OkHttpDispatcher().apply {
                maxRequests = THREAD_COUNT * 2
                maxRequestsPerHost = THREAD_COUNT * 2
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()

        okhttpVtClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(THREAD_COUNT * 2, 5L, TimeUnit.MINUTES))
            .dispatcher(okhttp3DispatcherWithVirtualThread().apply {
                maxRequests = THREAD_COUNT * 2
                maxRequestsPerHost = THREAD_COUNT * 2
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()

        jdkClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

        jdkVirtualClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build()

        hc5Classic = HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(THREAD_COUNT * 2)
                    .setMaxConnPerRoute(THREAD_COUNT * 2)
                    .build()
            )
            .build()

        hc5ClassicVt = virtualThreadHttpClientOf(
            maxConnTotal = THREAD_COUNT * 2,
            maxConnPerRoute = THREAD_COUNT * 2,
        )

        hc5Async = HttpAsyncClients.custom()
            .setConnectionManager(
                org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(THREAD_COUNT * 2)
                    .setMaxConnPerRoute(THREAD_COUNT * 2)
                    .build()
            )
            .build()
            .also { it.start() }

        ahcOptimizedClient = asyncHttpClient {
            setConnectTimeout(5_000)
            setRequestTimeout(10_000)
            setMaxConnectionsPerHost(THREAD_COUNT * 2)
            setMaxConnections(THREAD_COUNT * 4)
            setKeepAlive(true)
            setTcpNoDelay(true)
        }

        vertx = Vertx.vertx()
        vertxWebClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setKeepAlive(true)
                .setConnectTimeout(5_000)
                .setIdleTimeout(10)
        )
    }

    @TearDown
    fun teardown() {
        runCatching { vertxWebClient.close() }
        runCatching { runBlocking { vertx.close().coAwait() } }
        runCatching { ahcOptimizedClient.close() }
        runCatching { hc5Async.close() }
        runCatching { hc5Classic.close() }
        runCatching { hc5ClassicVt.close() }
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpVtClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpVtClient.connectionPool.evictAll() }
        wireMock.resetAll()
    }

    @Benchmark
    fun okhttp3Sync(): Int {
        val request = Request.Builder().url(delayUrl).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun okhttp3VirtualThread(): Int {
        val request = Request.Builder().url(delayUrl).get().build()
        okhttpVtClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun javaHttpSync(): Int {
        val request = HttpRequest.newBuilder(URI.create(delayUrl)).GET().build()
        return jdkClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).statusCode()
    }

    @Benchmark
    fun javaHttpVirtualThread(): Int {
        val request = HttpRequest.newBuilder(URI.create(delayUrl)).GET().build()
        return jdkVirtualClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).statusCode()
    }

    @Benchmark
    fun hc5Classic(): Int {
        val request = HttpGet(delayUrl)
        return hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5ClassicCoroutines(): Int = runBlocking {
        withContext(Dispatchers.IO) {
            val request = HttpGet(delayUrl)
            hc5Classic.execute(request) { response ->
                EntityUtils.consume(response.entity)
                response.code
            }
        }
    }

    @Benchmark
    fun hc5ClassicVirtualThread(): Int {
        val request = HttpGet(delayUrl)
        return hc5ClassicVt.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5AsyncCoroutines(): Int = runBlocking(Dispatchers.Default) {
        val request: SimpleHttpRequest = SimpleRequestBuilder.get(delayUrl).build()
        val response: SimpleHttpResponse = suspendCancellableCoroutine { cont ->
            val future = hc5Async.execute(
                request,
                object: FutureCallback<SimpleHttpResponse> {
                    override fun completed(result: SimpleHttpResponse) = cont.resume(result)
                    override fun failed(ex: Exception) = cont.resumeWithException(ex)
                    override fun cancelled() { cont.cancel() }
                }
            )
            cont.invokeOnCancellation { future.cancel(true) }
        }
        response.code
    }

    @Benchmark
    fun ahcOptimizedCoroutines(): Int = runBlocking(Dispatchers.Default) {
        ahcOptimizedClient.prepareGet(delayUrl).executeSuspending().statusCode
    }

    @Benchmark
    fun ahcOptimizedCoroutinesUnconfined(): Int = runBlocking(Dispatchers.Unconfined) {
        ahcOptimizedClient.prepareGet(delayUrl).executeSuspending().statusCode
    }

    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        vertxWebClient
            .get(serverPort, serverHost, "/delayed")
            .send()
            .coAwait()
            .statusCode()
    }
}
