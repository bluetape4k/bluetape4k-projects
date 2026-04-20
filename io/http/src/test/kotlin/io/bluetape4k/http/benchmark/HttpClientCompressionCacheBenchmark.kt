package io.bluetape4k.http.benchmark

import io.bluetape4k.http.hc5.cache.cachingHttpClient
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.openjdk.jmh.annotations.Threads
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

/**
 * 압축(gzip) + 응답 캐싱 시나리오 벤치마크.
 *
 * 베이스라인 [HttpClientBenchmark] 는 4-byte "pong" 응답이므로 캐시/압축 효과가 측정 불가.
 * 본 벤치마크는 ~1KB JSON payload + `Content-Encoding: gzip` + `Cache-Control: max-age=3600`
 * 응답을 MockWebServer 가 반환하도록 하여, 다음 4개 방법을 비교한다.
 *
 * - [okhttp3NoCache] : 캐시 미사용 OkHttp3 클라이언트 (매 요청 네트워크 + gzip 해제)
 * - [okhttp3WithCache] : `okhttp3.Cache` 로컬 디스크 캐시 (두 번째 이후 cache hit)
 * - [hc5NoCache] : 캐시 미사용 Apache HC5 Classic
 * - [hc5WithCache] : Apache HC5 `CachingHttpClient` 메모리 캐시
 *
 * @see io.bluetape4k.http.hc5.cache.cachingHttpClient
 * @see io.bluetape4k.http.okhttp3.CachingResponseInterceptor
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Threads(8)
open class HttpClientCompressionCacheBenchmark {

    private lateinit var mockServer: MockWebServer
    private lateinit var baseUrl: String

    private lateinit var okhttpClient: OkHttpClient
    private lateinit var okhttpCachedClient: OkHttpClient
    private lateinit var hc5Client: CloseableHttpClient
    private lateinit var hc5CachedClient: CloseableHttpClient

    private lateinit var cacheDir: File

    @Setup
    fun setup() {
        // ~1KB JSON payload
        val jsonBody = """{"data":"${"x".repeat(980)}"}"""
        val gzipBytes = gzip(jsonBody)

        mockServer = MockWebServer().apply {
            dispatcher = object: Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Content-Encoding", "gzip")
                        .addHeader("Cache-Control", "public, max-age=3600")
                        .setBody(Buffer().write(gzipBytes))
            }
            start()
        }
        baseUrl = mockServer.url("/data").toString()

        okhttpClient = OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build()

        cacheDir = File(
            System.getProperty("java.io.tmpdir"),
            "okhttp-cache-${System.nanoTime()}"
        ).apply { mkdirs() }

        okhttpCachedClient = OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .build()

        hc5Client = HttpClients.createDefault()

        // Apache HC5 CachingHttpClient — in-memory cache (default when no storage specified)
        hc5CachedClient = cachingHttpClient { }
    }

    @TearDown
    fun teardown() {
        runCatching { okhttpClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpClient.connectionPool.evictAll() }
        runCatching { okhttpCachedClient.dispatcher.executorService.shutdown() }
        runCatching { okhttpCachedClient.connectionPool.evictAll() }
        runCatching { okhttpCachedClient.cache?.close() }
        runCatching { hc5Client.close() }
        runCatching { hc5CachedClient.close() }
        runCatching { mockServer.shutdown() }
        runCatching { cacheDir.deleteRecursively() }
    }

    /**
     * OkHttp3 캐시 미사용: 매 요청마다 네트워크 + gzip 해제.
     * OkHttp3 는 자동으로 `Accept-Encoding: gzip` 을 붙이지 않는 경우 transparent gzip 해제를 수행.
     */
    @Benchmark
    fun okhttp3NoCache(): Int {
        val request = Request.Builder().url(baseUrl).get().build()
        okhttpClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    /**
     * OkHttp3 + DiskLruCache: 동일 URL 반복 요청 시 캐시 히트.
     */
    @Benchmark
    fun okhttp3WithCache(): Int {
        val request = Request.Builder().url(baseUrl).get().build()
        okhttpCachedClient.newCall(request).execute().use { response ->
            response.body.bytes()
            return response.code
        }
    }

    /**
     * HC5 Classic 캐시 미사용.
     */
    @Benchmark
    fun hc5NoCache(): Int {
        val request = HttpGet(baseUrl)
        return hc5Client.execute(request) { response ->
            EntityUtils.consume(response.entity)
            response.code
        }
    }

    /**
     * HC5 CachingHttpClient: 동일 URL 반복 요청 시 캐시 히트.
     */
    @Benchmark
    fun hc5WithCache(): Int {
        val request = HttpGet(baseUrl)
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
