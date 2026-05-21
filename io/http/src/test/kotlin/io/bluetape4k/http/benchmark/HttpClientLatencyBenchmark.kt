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
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.openjdk.jmh.annotations.Threads
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.Dispatcher as OkHttpDispatcher

/**
 * 고지연(~50 ms) 환경에서 동기 vs 비동기 HTTP 클라이언트 처리량 비교.
 *
 * Docker 기반 [BluetapeWebfluxServer] 의 `/httpbin/delay/0.05` 엔드포인트를 사용해
 * 서버 JVM 분리 + 50ms 지연을 시뮬레이션합니다.
 *
 * 동기 클라이언트 이론 상한: threads × (1000 / 50ms) = 2,000 ops/s
 * 비동기는 스레드 블로킹 없이 더 높은 동시성 확보 가능.
 *
 * Vert.x 5 defaults to a 5-connection HTTP/1 pool. This benchmark configures
 * the pool to match the other clients so the high-latency test compares client
 * behavior instead of the default connection cap.
 *
 * Ktor CIO 3.5 opens dedicated HTTP/1 connections when its pipeline path is
 * disabled. The benchmark therefore keeps a short equal-thread measurement
 * window for every client instead of limiting only the CIO row to one thread.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(100)
open class HttpClientLatencyBenchmark {

    private val server = BluetapeWebfluxServer.Launcher.bluetapeWebfluxServer

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
    private lateinit var ktorCioClient: KtorHttpClient
    private lateinit var vertx: Vertx
    private lateinit var vertxWebClient: WebClient

    @Setup
    fun setup() {
        delayUrl = "${server.url}/httpbin/delay/0.05"
        serverHost = server.host
        serverPort = server.port

        val connPerHost = 200

        okhttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(OkHttpDispatcher().apply {
                maxRequests = connPerHost
                maxRequestsPerHost = connPerHost
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()

        okhttpVtClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(okhttp3DispatcherWithVirtualThread().apply {
                maxRequests = connPerHost
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
                    .setMaxConnTotal(connPerHost)
                    .setMaxConnPerRoute(connPerHost)
                    .build()
            )
            .build()

        hc5ClassicVt = virtualThreadHttpClientOf(
            maxConnTotal = connPerHost,
            maxConnPerRoute = connPerHost,
        )

        hc5Async = HttpAsyncClients.custom()
            .setConnectionManager(
                PoolingAsyncClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(connPerHost)
                    .setMaxConnPerRoute(connPerHost)
                    .build()
            )
            .build()
            .also { it.start() }

        ktorCioClient = ktorCioHttpClientOf {
            engine {
                maxConnectionsCount = connPerHost
                requestTimeout = 10_000
                endpoint.maxConnectionsPerRoute = connPerHost
                endpoint.connectTimeout = 5_000
                endpoint.socketTimeout = 10_000
                endpoint.keepAliveTime = 5_000
            }
        }

        vertx = Vertx.vertx()
        vertxWebClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setKeepAlive(true)
                .setConnectTimeout(5_000)
                .setIdleTimeout(10)
                .setDecompressionSupported(true),
            PoolOptions()
                .setHttp1MaxSize(connPerHost)
                .setHttp2MaxSize(connPerHost)
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
    fun okhttp3Coroutines(): Int = runBlocking(Dispatchers.IO) {
        val request = Request.Builder().url(delayUrl).get().build()
        okhttpClient.newCall(request).executeAsync().use { response ->
            response.body.bytes()
            response.code
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
    fun javaHttpCoroutines(): Int = runBlocking(Dispatchers.IO) {
        jdkClient.sendAwait(
            HttpRequest.newBuilder(URI.create(delayUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray()
        ).statusCode()
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
    fun hc5ClassicCoroutines(): Int = runBlocking(Dispatchers.IO) {
        val request = HttpGet(delayUrl)
        hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
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
    fun hc5AsyncCoroutines(): Int = runBlocking(Dispatchers.IO) {
        val request = SimpleRequestBuilder.get(delayUrl).build()
        hc5Async.executeSuspending(request).code
    }

    @Benchmark
    fun ktorCioCoroutines(): Int = runBlocking {
        val response = ktorCioClient.get(delayUrl)
        response.bodyAsBytes()
        response.status.value
    }

    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        vertxWebClient
            .get(serverPort, serverHost, "/httpbin/delay/0.05")
            .send()
            .coAwait()
            .statusCode()
    }
}
