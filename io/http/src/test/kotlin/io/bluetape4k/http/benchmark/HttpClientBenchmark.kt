package io.bluetape4k.http.benchmark

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
open class HttpClientBenchmark {

    private lateinit var mockServer: MockWebServer
    private lateinit var baseUrl: String
    private var mockPort: Int = 0

    // clients
    private lateinit var okhttpClient: OkHttpClient
    private lateinit var okhttpVtClient: OkHttpClient
    private lateinit var jdkClient: HttpClient
    private lateinit var jdkVirtualClient: HttpClient
    private lateinit var hc5Classic: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5ClassicVt: org.apache.hc.client5.http.impl.classic.CloseableHttpClient
    private lateinit var hc5Async: org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
    private lateinit var ahcClient: org.asynchttpclient.AsyncHttpClient
    private lateinit var vertx: Vertx
    private lateinit var vertxWebClient: WebClient

    @Setup
    fun setup() {
        mockServer = MockWebServer().apply {
            dispatcher = object: Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    MockResponse().setResponseCode(200).setBody("pong")
            }
            start()
        }
        mockPort = mockServer.port
        baseUrl = mockServer.url("/ping").toString()

        okhttpClient = OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build()

        okhttpVtClient = OkHttpClient.Builder()
            .dispatcher(okhttp3DispatcherWithVirtualThread())
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

        hc5Classic = HttpClients.createDefault()

        hc5ClassicVt = virtualThreadHttpClientOf()

        hc5Async = HttpAsyncClients.createDefault().also { it.start() }

        ahcClient = DefaultAsyncHttpClient(
            DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(5_000)
                .setRequestTimeout(5_000)
                .build()
        )

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
        runCatching { ahcClient.close() }
        runCatching { hc5Async.close() }
        runCatching { hc5Classic.close() }
        runCatching { hc5ClassicVt.close() }
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpVtClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpVtClient.connectionPool.evictAll() }
        runCatching { mockServer.shutdown() }
    }

    @Benchmark
    fun okhttp3Sync(): Int {
        val request = Request.Builder().url(baseUrl).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun javaHttpSync(): Int {
        val request = HttpRequest.newBuilder(URI.create(baseUrl)).GET().build()
        val response = jdkClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun javaHttpVirtualThread(): Int {
        val request = HttpRequest.newBuilder(URI.create(baseUrl)).GET().build()
        val response = jdkVirtualClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return response.statusCode()
    }

    @Benchmark
    fun hc5Classic(): Int {
        val request = HttpGet(baseUrl)
        return hc5Classic.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    /** HC5 Classic 을 Coroutines IO Dispatcher 에서 실행 — 블로킹 코드의 코루틴 래핑 패턴 */
    @Benchmark
    fun hc5ClassicCoroutines(): Int = runBlocking {
        withContext(Dispatchers.IO) {
            val request = HttpGet(baseUrl)
            hc5Classic.execute(request) { response ->
                EntityUtils.consume(response.entity)
                response.code
            }
        }
    }

    @Benchmark
    fun hc5AsyncCoroutines(): Int = runBlocking(Dispatchers.Default) {
        val request: SimpleHttpRequest = SimpleRequestBuilder.get(baseUrl).build()
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
            val future = ahcClient.prepareGet(baseUrl).execute().toCompletableFuture()
            future.whenComplete { response, error ->
                if (error != null) cont.resumeWithException(error)
                else cont.resume(response.statusCode)
            }
            cont.invokeOnCancellation { future.cancel(true) }
        }
    }

    /** Vert.x WebClient + Coroutines — 이벤트 루프 기반 비동기 HTTP */
    @Benchmark
    fun vertxWebClientCoroutines(): Int = runBlocking {
        val response = vertxWebClient
            .get(mockPort, "localhost", "/ping")
            .send()
            .coAwait()
        response.statusCode()
    }

    @Benchmark
    fun okhttp3VirtualThread(): Int {
        val request = Request.Builder().url(baseUrl).get().build()
        okhttpVtClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    @Benchmark
    fun hc5ClassicVirtualThread(): Int {
        val request = HttpGet(baseUrl)
        return hc5ClassicVt.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }
}
