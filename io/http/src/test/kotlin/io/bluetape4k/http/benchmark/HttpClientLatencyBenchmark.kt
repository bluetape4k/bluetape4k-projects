package io.bluetape4k.http.benchmark

import com.github.tomakehurst.wiremock.client.WireMock
import io.bluetape4k.http.ahc.asyncHttpClient
import okhttp3.coroutines.executeAsync
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
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
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
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 고지연(~50 ms) 환경에서 동기 vs 비동기 HTTP 클라이언트 처리량 비교.
 *
 * - WireMock Docker 서버 N개를 사용해 서버 병목을 제거합니다.
 * - [DELAY_MS] ms fixed delay stub으로 지연을 시뮬레이션합니다.
 * - @Threads(100) + 다수 서버로 진짜 고동시성 구간을 측정합니다.
 *
 * 동기 클라이언트 이론 상한: threads × (1000 / DELAY_MS) ops/s
 * 100 스레드 × 20 req/s = 2,000 ops/s (단일 서버 기준)
 * N 서버 시: N × 2,000 ops/s 까지 확장
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(100)
open class HttpClientLatencyBenchmark {

    companion object {
        const val DELAY_MS = 50
    }

    private lateinit var wireMocks: List<WireMockServer>
    private lateinit var delayUrls: List<String>
    private lateinit var serverHosts: List<String>
    private lateinit var serverPorts: IntArray

    private fun pickUrl(): String = delayUrls[ThreadLocalRandom.current().nextInt(delayUrls.size)]
    private fun pickIdx(): Int = ThreadLocalRandom.current().nextInt(wireMocks.size)

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
        // WireMock 서버 N개 — 서버 병목 제거
        val n = minOf(Runtime.getRuntime().availableProcessors(), 4)
        wireMocks = List(n) {
            WireMockServer().apply {
                start()
                stubFor(
                    WireMock.get("/delayed")
                        .willReturn(WireMock.ok("pong").withFixedDelay(DELAY_MS))
                )
            }
        }
        delayUrls = wireMocks.map { "${it.url}/delayed" }
        serverHosts = wireMocks.map { it.host }
        serverPorts = wireMocks.map { it.port }.toIntArray()

        val connPerHost = 200

        okhttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(n * connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(OkHttpDispatcher().apply {
                maxRequests = n * connPerHost
                maxRequestsPerHost = connPerHost
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()

        okhttpVtClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(n * connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(okhttp3DispatcherWithVirtualThread().apply {
                maxRequests = n * connPerHost
                maxRequestsPerHost = connPerHost
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
                    .setMaxConnTotal(n * connPerHost)
                    .setMaxConnPerRoute(connPerHost)
                    .build()
            )
            .build()

        hc5ClassicVt = virtualThreadHttpClientOf(
            maxConnTotal = n * connPerHost,
            maxConnPerRoute = connPerHost,
        )

        hc5Async = HttpAsyncClients.custom()
            .setConnectionManager(
                PoolingAsyncClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(n * connPerHost)
                    .setMaxConnPerRoute(connPerHost)
                    .build()
            )
            .build()
            .also { it.start() }

        ahcOptimizedClient = asyncHttpClient {
            setConnectTimeout(5_000)
            setRequestTimeout(10_000)
            setMaxConnectionsPerHost(connPerHost)
            setMaxConnections(n * connPerHost)
            setKeepAlive(true)
            setTcpNoDelay(true)
        }

        vertx = Vertx.vertx()
        vertxWebClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setMaxPoolSize(connPerHost)   // 기본값 5 → 200으로 확장
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
        wireMocks.forEach { runCatching { it.resetAll() } }
        wireMocks.forEach { runCatching { it.stop() } }
    }

    @Benchmark
    fun okhttp3Sync(): Int {
        val request = Request.Builder().url(pickUrl()).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun okhttp3VirtualThread(): Int {
        val request = Request.Builder().url(pickUrl()).get().build()
        okhttpVtClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun okhttp3Coroutines(): Int = runBlocking(Dispatchers.Default) {
        val request = Request.Builder().url(pickUrl()).get().build()
        okhttpClient.newCall(request).executeAsync().use { response ->
            response.body.bytes()
            response.code
        }
    }

    @Benchmark
    fun javaHttpSync(): Int {
        val request = HttpRequest.newBuilder(URI.create(pickUrl())).GET().build()
        return jdkClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).statusCode()
    }

    @Benchmark
    fun javaHttpVirtualThread(): Int {
        val request = HttpRequest.newBuilder(URI.create(pickUrl())).GET().build()
        return jdkVirtualClient.send(request, HttpResponse.BodyHandlers.ofByteArray()).statusCode()
    }

    @Benchmark
    fun hc5Classic(): Int {
        val request = HttpGet(pickUrl())
        return hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5ClassicCoroutines(): Int = runBlocking {
        withContext(Dispatchers.IO) {
            val request = HttpGet(pickUrl())
            hc5Classic.execute(request) { response ->
                EntityUtils.consume(response.entity)
                response.code
            }
        }
    }

    @Benchmark
    fun hc5ClassicVirtualThread(): Int {
        val request = HttpGet(pickUrl())
        return hc5ClassicVt.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5AsyncCoroutines(): Int = runBlocking(Dispatchers.Default) {
        val request: SimpleHttpRequest = SimpleRequestBuilder.get(pickUrl()).build()
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
        ahcOptimizedClient.prepareGet(pickUrl()).executeSuspending().statusCode
    }

    @Benchmark
    fun ahcOptimizedCoroutinesUnconfined(): Int = runBlocking(Dispatchers.Unconfined) {
        ahcOptimizedClient.prepareGet(pickUrl()).executeSuspending().statusCode
    }

    /** Vert.x WebClient — getAbs()로 다중 서버 라운드로빈 */
    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        val idx = pickIdx()
        vertxWebClient
            .get(serverPorts[idx], serverHosts[idx], "/delayed")
            .send()
            .coAwait()
            .statusCode()
    }
}
