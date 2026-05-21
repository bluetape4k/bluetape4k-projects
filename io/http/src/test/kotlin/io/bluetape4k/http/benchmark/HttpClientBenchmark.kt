package io.bluetape4k.http.benchmark

import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf
import io.bluetape4k.http.jdk.sendAwait
import io.bluetape4k.http.ktor.ktorCioHttpClientOf
import io.bluetape4k.http.okhttp3.okhttp3DispatcherWithVirtualThread
import io.bluetape4k.testcontainers.http.BluetapeWebfluxServer
import io.ktor.client.HttpClient as KtorHttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.vertx.core.Vertx
import io.vertx.core.http.PoolOptions
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
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher as OkHttpDispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.openjdk.jmh.annotations.Threads
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 다양한 HTTP Client 의 throughput 을 비교하는 JMH 벤치마크.
 *
 * Docker 기반 [BluetapeWebfluxServer] 를 외부 프로세스로 사용하므로
 * 서버 JVM 이 벤치마크 JVM 과 분리되어 클라이언트 단독 성능을 측정합니다.
 *
 * 엔드포인트: `GET /ping` → HTTP 200 (경량 응답, 순수 연결 처리량 측정)
 *
 * 비교 대상:
 * - OkHttp3 동기 / Virtual Thread / Coroutines
 * - java.net.http 동기 / Virtual Thread / HTTP2 / Coroutines
 * - Apache HC5 Classic 동기 / Coroutines / Virtual Thread
 * - Apache HC5 Async + Coroutines
 * - Ktor CIO + Coroutines
 * - Vert.x WebClient + Coroutines
 *
 * Ktor CIO 3.5 opens dedicated HTTP/1 connections when its pipeline path is
 * disabled. The benchmark therefore keeps a short equal-thread measurement
 * window for every client instead of limiting only the CIO row to one thread.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(8)
open class HttpClientBenchmark {

    // 별도 Docker 프로세스 — 벤치마크 JVM 과 자원 경쟁 없음
    private val server = BluetapeWebfluxServer.Launcher.bluetapeWebfluxServer

    private lateinit var pingUrl: String
    private lateinit var serverHost: String
    private var serverPort: Int = 0

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
    private lateinit var ktorCioClient: KtorHttpClient
    private lateinit var vertx: Vertx
    private lateinit var vertxWebClient: WebClient

    @Setup
    fun setup() {
        val n = Runtime.getRuntime().availableProcessors()
        val maxConnections = n * 50

        pingUrl = "${server.url}/ping"
        serverHost = server.host
        serverPort = server.port

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

        ktorCioClient = ktorCioHttpClientOf {
            engine {
                maxConnectionsCount = maxConnections
                requestTimeout = 5_000
                endpoint.maxConnectionsPerRoute = maxConnections
                endpoint.connectTimeout = 5_000
                endpoint.socketTimeout = 5_000
                endpoint.keepAliveTime = 5_000
            }
        }

        vertx = Vertx.vertx()
        vertxWebClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setKeepAlive(true)
                .setConnectTimeout(5_000)
                .setIdleTimeout(5),
            PoolOptions()
                .setHttp1MaxSize(maxConnections)
                .setHttp2MaxSize(maxConnections)
        )
    }

    @TearDown
    fun teardown() {
        runCatching { ktorCioClient.close() }
        runCatching { vertxWebClient.close() }
        runCatching { runBlocking { vertx.close().coAwait() } }
        runCatching { hc5Async.close() }
        runCatching { hc5Classic.close() }
        runCatching { hc5ClassicVt.close() }
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpVtClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpVtClient.connectionPool.evictAll() }
    }

    @Benchmark
    fun okhttp3Sync(): Int {
        val request = Request.Builder().url(pingUrl).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun okhttp3VirtualThread(): Int {
        val request = Request.Builder().url(pingUrl).get().build()
        okhttpVtClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun okhttp3Coroutines(): Int = runBlocking(Dispatchers.IO) {
        val request = Request.Builder().url(pingUrl).get().build()
        okhttpClient.newCall(request).executeAsync().use { response ->
            response.body.bytes()
            response.code
        }
    }

    @Benchmark
    fun javaHttpSync(): Int {
        val request = HttpRequest.newBuilder(URI.create(pingUrl)).GET().build()
        val response = jdkClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpVirtualThread(): Int {
        val request = HttpRequest.newBuilder(URI.create(pingUrl)).GET().build()
        val response = jdkVirtualClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpH2Sync(): Int {
        val request = HttpRequest.newBuilder().uri(URI.create(pingUrl)).GET().build()
        val response = jdkH2Client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpH2VirtualThread(): Int {
        val request = HttpRequest.newBuilder().uri(URI.create(pingUrl)).GET().build()
        val response = jdkH2VirtualClient.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpCoroutines(): Int = runBlocking(Dispatchers.IO) {
        jdkClient.sendAwait(
            HttpRequest.newBuilder(URI.create(pingUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray()
        ).statusCode()
    }

    @Benchmark
    fun javaHttpH2Coroutines(): Int = runBlocking(Dispatchers.IO) {
        jdkH2Client.sendAwait(
            HttpRequest.newBuilder(URI.create(pingUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray()
        ).statusCode()
    }

    @Benchmark
    fun hc5Classic(): Int {
        val request = HttpGet(pingUrl)
        return hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5ClassicCoroutines(): Int = runBlocking {
        withContext(Dispatchers.IO) {
            val request = HttpGet(pingUrl)
            hc5Classic.execute(request) { response ->
                EntityUtils.consume(response.entity)
                response.code
            }
        }
    }

    @Benchmark
    fun hc5ClassicVirtualThread(): Int {
        val request = HttpGet(pingUrl)
        return hc5ClassicVt.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    @Benchmark
    fun hc5AsyncCoroutines(): Int = runBlocking(Dispatchers.IO) {
        val request = SimpleRequestBuilder.get(pingUrl).build()
        hc5Async.executeSuspending(request).code
    }

    @Benchmark
    fun ktorCioCoroutines(): Int = runBlocking {
        val response = ktorCioClient.get(pingUrl)
        response.bodyAsBytes()
        response.status.value
    }

    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        vertxWebClient
            .get(serverPort, serverHost, "/ping")
            .send()
            .coAwait()
            .statusCode()
    }
}
