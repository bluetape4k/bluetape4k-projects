package io.bluetape4k.http.benchmark

import io.bluetape4k.http.ahc.asyncHttpClient
import io.bluetape4k.http.ahc.executeSuspending
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf
import io.bluetape4k.http.okhttp3.okhttp3DispatcherWithVirtualThread
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
import org.openjdk.jmh.annotations.Threads
import okhttp3.ConnectionPool
import okhttp3.Dispatcher as OkHttpDispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
 * 다양한 HTTP Client 들의 throughput 을 비교하는 JMH 벤치마크.
 *
 * 로컬 [MockWebServer] 를 하나 띄워서 고정된 200 응답을 반환하도록 하고,
 * 각 클라이언트가 동일 엔드포인트에 GET 요청을 수행합니다.
 *
 * 비교 대상:
 * - OkHttp3 동기
 * - java.net.http 동기
 * - java.net.http + Virtual Threads
 * - Apache HC5 Classic 동기
 * - Apache HC5 Classic + Coroutines (withContext(IO))
 * - Apache HC5 Async + Coroutines
 * - Apache AsyncHttpClient + Coroutines
 * - Vert.x WebClient + Coroutines
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Threads(8)
open class HttpClientBenchmark {

    private lateinit var mockServers: List<MockWebServer>
    private lateinit var baseUrls: List<String>
    private lateinit var mockPorts: IntArray

    private fun pickUrl(): String = baseUrls[ThreadLocalRandom.current().nextInt(baseUrls.size)]
    private fun pickPort(): Int = mockPorts[ThreadLocalRandom.current().nextInt(mockPorts.size)]

    // clients
    private lateinit var okhttpClient: OkHttpClient
    private lateinit var okhttpVtClient: OkHttpClient
    private lateinit var jdkClient: HttpClient
    private lateinit var jdkVirtualClient: HttpClient
    private lateinit var jdkH2Client: HttpClient
    private lateinit var jdkH2VirtualClient: HttpClient
    private lateinit var hc5Classic: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5ClassicVt: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5Async: org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
    private lateinit var ahcClient: org.asynchttpclient.AsyncHttpClient
    private lateinit var ahcOptimizedClient: org.asynchttpclient.AsyncHttpClient
    private lateinit var vertx: Vertx
    private lateinit var vertxWebClient: WebClient

    @Setup
    fun setup() {
        val n = Runtime.getRuntime().availableProcessors()
        val sharedDispatcher = object: Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(200)
        }
        mockServers = List(n) { MockWebServer().apply { dispatcher = sharedDispatcher; start() } }
        baseUrls = mockServers.map { it.url("/ping").toString() }
        mockPorts = mockServers.map { it.port }.toIntArray()

        okhttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(n * 20, 5L, TimeUnit.MINUTES))
            .dispatcher(OkHttpDispatcher().apply {
                maxRequests = n * 50
                maxRequestsPerHost = n * 50
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build()

        okhttpVtClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(n * 20, 5L, TimeUnit.MINUTES))
            .dispatcher(okhttp3DispatcherWithVirtualThread().apply {
                maxRequests = n * 50
                maxRequestsPerHost = n * 50
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build()

        jdkClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

        jdkVirtualClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build()

        jdkH2Client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build()

        jdkH2VirtualClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(Duration.ofSeconds(5))
            .build()

        hc5Classic = HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(500)
                    .setMaxConnPerRoute(200)
                    .build()
            )
            .build()

        hc5ClassicVt = virtualThreadHttpClientOf()

        hc5Async = HttpAsyncClients.createDefault().also { it.start() }

        ahcClient = DefaultAsyncHttpClient(
            DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(5_000)
                .setRequestTimeout(5_000)
                .build()
        )

        // Optimized AHC: native transport + connection pool tuning via asyncHttpClient DSL
        ahcOptimizedClient = asyncHttpClient {
            setConnectTimeout(5_000)
            setRequestTimeout(5_000)
            setMaxConnectionsPerHost(100)
            setMaxConnections(200)
            setKeepAlive(true)
            setTcpNoDelay(true)
        }

        vertx = Vertx.vertx()
        vertxWebClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setKeepAlive(true)
                .setConnectTimeout(5_000)
                .setIdleTimeout(5)
        )
    }

    @TearDown
    fun teardown() {
        runCatching { vertxWebClient.close() }
        runCatching { runBlocking { vertx.close().coAwait() } }
        runCatching { ahcOptimizedClient.close() }
        runCatching { ahcClient.close() }
        runCatching { hc5Async.close() }
        runCatching { hc5Classic.close() }
        runCatching { hc5ClassicVt.close() }
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpVtClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpVtClient.connectionPool.evictAll() }
        mockServers.forEach { runCatching { it.shutdown() } }
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
    fun javaHttpSync(): Int {
        val request = HttpRequest.newBuilder(URI.create(pickUrl())).GET().build()
        val response = jdkClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpVirtualThread(): Int {
        val request = HttpRequest.newBuilder(URI.create(pickUrl())).GET().build()
        val response = jdkVirtualClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpH2Sync(): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(pickUrl()))
            .GET()
            .build()
        val response = jdkH2Client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpH2VirtualThread(): Int {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(pickUrl()))
            .GET()
            .build()
        val response = jdkH2VirtualClient.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode()
    }

    @Benchmark
    fun hc5Classic(): Int {
        val request = HttpGet(pickUrl())
        return hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    /** HC5 Classic 을 Coroutines IO Dispatcher 에서 실행 — 블로킹 코드의 코루틴 래핑 패턴 */
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
    fun ahcCoroutines(): Int = runBlocking(Dispatchers.Default) {
        suspendCancellableCoroutine { cont ->
            val future = ahcClient.prepareGet(pickUrl()).execute().toCompletableFuture()
            future.whenComplete { response, error ->
                if (error != null) cont.resumeWithException(error)
                else cont.resume(response.statusCode)
            }
            cont.invokeOnCancellation { future.cancel(true) }
        }
    }

    /** AHC + executeSuspending() + native transport + pool tuning */
    @Benchmark
    fun ahcOptimizedCoroutines(): Int = runBlocking(Dispatchers.Default) {
        ahcOptimizedClient.prepareGet(pickUrl()).executeSuspending().statusCode
    }

    /** Vert.x WebClient + Coroutines — 이벤트 루프 기반 비동기 HTTP */
    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        val response = vertxWebClient
            .get(pickPort(), "localhost", "/ping")
            .send()
            .coAwait()
        response.statusCode()
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
    fun hc5ClassicVirtualThread(): Int {
        val request = HttpGet(pickUrl())
        return hc5ClassicVt.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }
}
