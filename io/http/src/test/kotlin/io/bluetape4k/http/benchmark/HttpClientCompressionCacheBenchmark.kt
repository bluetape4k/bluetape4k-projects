package io.bluetape4k.http.benchmark

import com.github.tomakehurst.wiremock.client.WireMock
import io.bluetape4k.http.hc5.cache.memoryCachingHttpClientOf
import io.bluetape4k.http.hc5.classic.virtualThreadHttpClientOf
import io.bluetape4k.testcontainers.http.WireMockServer
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.benchmark.Warmup
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.openjdk.jmh.annotations.Threads
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

/**
 * gzip 압축 + HTTP 응답 캐시 영향도 벤치마크.
 *
 * WireMock Docker 외부 서버에 10ms 지연 + `Cache-Control: public, max-age=3600` + gzip 응답을
 * 설정하여, 실제 네트워크 왕복이 있는 상황에서 캐시 효과를 측정합니다.
 *
 * - **NoCache**: 요청마다 서버 왕복 + gzip 해제 → 지연 누적
 * - **WithMemCache** (HC5 CachingHttpClient): 첫 요청 후 메모리에서 즉시 응답
 * - **WithDiskCache** (OkHttp DiskLruCache): 첫 요청 후 디스크 캐시에서 응답
 *
 * 이론값:
 * - NoCache:  threads × (1000 / 10ms) = 8 × 100 = 800 ops/s
 * - WithCache: 서버 왕복 없음 → 수만 ops/s (메모리/디스크 속도에 의존)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(8)
open class HttpClientCompressionCacheBenchmark {

    companion object {
        const val DELAY_MS = 10
    }

    private lateinit var wireMock: WireMockServer
    private lateinit var dataUrl: String

    private lateinit var okhttpClient: OkHttpClient
    private lateinit var okhttpCachedClient: OkHttpClient
    private lateinit var hc5Client: CloseableHttpClient
    private lateinit var hc5VtClient: CloseableHttpClient
    private lateinit var hc5CachedClient: CloseableHttpClient
    private lateinit var cacheDir: File

    @Setup
    fun setup() {
        // ~1 KB JSON payload + gzip 압축
        val jsonBody = """{"data":"${"x".repeat(980)}"}"""
        val gzipBytes = gzip(jsonBody)

        wireMock = WireMockServer().apply {
            start()
            stubFor(
                WireMock.get("/cached-data")
                    .willReturn(
                        WireMock.ok()
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Content-Encoding", "gzip")
                            .withHeader("Cache-Control", "public, max-age=3600")
                            .withFixedDelay(DELAY_MS)
                            .withBody(gzipBytes)
                    )
            )
        }
        dataUrl = "${wireMock.url}/cached-data"

        val connPerHost = 100

        okhttpClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                maxRequests = connPerHost
                maxRequestsPerHost = connPerHost
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build()

        cacheDir = File(
            System.getProperty("java.io.tmpdir"),
            "okhttp-bench-cache-${System.nanoTime()}"
        ).apply { mkdirs() }

        okhttpCachedClient = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(connPerHost, 5L, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                maxRequests = connPerHost
                maxRequestsPerHost = connPerHost
            })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .build()

        hc5Client = HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(connPerHost)
                    .setMaxConnPerRoute(connPerHost)
                    .build()
            )
            .build()

        hc5VtClient = virtualThreadHttpClientOf(
            maxConnTotal = connPerHost,
            maxConnPerRoute = connPerHost,
        )

        hc5CachedClient = memoryCachingHttpClientOf()
    }

    @TearDown
    fun teardown() {
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpCachedClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpCachedClient.connectionPool.evictAll() }
        runCatching { okhttpCachedClient.cache?.close() }
        runCatching { hc5Client.close() }
        runCatching { hc5VtClient.close() }
        runCatching { hc5CachedClient.close() }
        runCatching { wireMock.resetAll() }
        runCatching { wireMock.stop() }
        runCatching { cacheDir.deleteRecursively() }
    }

    /** OkHttp3: 캐시 없음 — 매 요청마다 서버 왕복 + gzip 해제 */
    @Benchmark
    fun okhttp3NoCache(): Int {
        val request = Request.Builder().url(dataUrl).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    /** OkHttp3: DiskLruCache — 첫 요청 후 디스크 캐시 히트 */
    @Benchmark
    fun okhttp3WithDiskCache(): Int {
        val request = Request.Builder().url(dataUrl).get().build()
        okhttpCachedClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    /** HC5 Classic: 캐시 없음 — 매 요청마다 서버 왕복 */
    @Benchmark
    fun hc5ClassicNoCache(): Int {
        val request = HttpGet(dataUrl)
        return hc5Client.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    /** HC5 Classic Virtual Thread: 캐시 없음 */
    @Benchmark
    fun hc5ClassicVtNoCache(): Int {
        val request = HttpGet(dataUrl)
        return hc5VtClient.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    /** HC5 CachingHttpClient: 인메모리 캐시 히트 */
    @Benchmark
    fun hc5ClassicWithMemCache(): Int {
        val request = HttpGet(dataUrl)
        return hc5CachedClient.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }
}

private fun gzip(text: String): ByteArray {
    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    return baos.toByteArray()
}
