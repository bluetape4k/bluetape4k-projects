package io.bluetape4k.http.okhttp3

import com.github.tomakehurst.wiremock.client.WireMock
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.http.WireMockServer
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

/**
 * OkHttp DiskLruCache 가 실제로 캐시 히트를 반환하는지 검증.
 *
 * 35K ops/s 의심 원인 분석:
 * - "Disk" 캐시지만 1KB 파일은 워밍업 후 OS 페이지 캐시(RAM)에 올라감
 * - 네트워크 왕복(10ms) 제거가 핵심 → no-cache 660 ops/s → disk cache 35K ops/s (53배)
 * - DiskLruCache synchronized + journal write 오버헤드로 HC5 MemCache(813K)보다 23배 느림
 */
class OkHttpDiskCacheVerificationTest {

    companion object: KLogging()

    private lateinit var wireMock: WireMockServer
    private lateinit var cacheDir: File
    private lateinit var client: OkHttpClient
    private lateinit var dataUrl: String

    @BeforeEach
    fun setup() {
        val jsonBody = """{"data":"${"x".repeat(980)}"}"""
        val gzipBytes = gzip(jsonBody)

        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val httpDate = now.format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val lastModified = now.minusDays(1).format(DateTimeFormatter.RFC_1123_DATE_TIME)

        wireMock = WireMockServer().apply {
            start()
            stubFor(
                WireMock.get("/cached-data")
                    .willReturn(
                        WireMock.ok()
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Content-Encoding", "gzip")
                            .withHeader("Cache-Control", "public, max-age=3600")
                            .withHeader("Date", httpDate)
                            .withHeader("Last-Modified", lastModified)
                            .withHeader("Vary", "Accept-Encoding")
                            .withFixedDelay(10)
                            .withBody(gzipBytes)
                    )
            )
        }
        dataUrl = "${wireMock.url}/cached-data"

        cacheDir = File(System.getProperty("java.io.tmpdir"), "okhttp-verify-${System.nanoTime()}").apply { mkdirs() }
        client = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 5L, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply { maxRequests = 50; maxRequestsPerHost = 50 })
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .build()
    }

    @AfterEach
    fun teardown() {
        val cache = client.cache
        if (cache != null) {
            log.info {
                """
                |=== OkHttp DiskLruCache Stats ===
                | requestCount : ${cache.requestCount()}
                | hitCount     : ${cache.hitCount()}
                | networkCount : ${cache.networkCount()}
                | hitRate      : ${"%.1f".format(cache.hitCount() * 100.0 / cache.requestCount().coerceAtLeast(1))}%
                """.trimMargin()
            }
        }
        runCatching { client.dispatcher.executorService.shutdown() }
        runCatching { client.cache?.close() }
        runCatching { wireMock.stop() }
        runCatching { cacheDir.deleteRecursively() }
    }

    @Test
    fun `첫 번째 요청은 네트워크, 이후는 캐시 히트`() {
        val request = Request.Builder().url(dataUrl).get().build()

        // 1st request → network miss
        client.newCall(request).execute().use { r ->
            r.body.bytes()
            log.info { "1st: code=${r.code}, cacheResponse=${r.cacheResponse != null}, networkResponse=${r.networkResponse != null}" }
        }

        // 2nd ~ 20th request → should all be cache hits
        repeat(19) {
            client.newCall(request).execute().use { r ->
                r.body.bytes()
                (r.networkResponse == null).shouldBeTrue()   // 캐시 히트 = 네트워크 없음
                (r.cacheResponse != null).shouldBeTrue()     // 캐시 응답 존재
            }
        }

        val cache = client.cache!!
        log.info { "hitCount=${cache.hitCount()}, networkCount=${cache.networkCount()}, requestCount=${cache.requestCount()}" }
        cache.hitCount() shouldBeGreaterThan 0
    }

    @Test
    fun `캐시 히트 속도는 no-cache 보다 훨씬 빠르다`() {
        val request = Request.Builder().url(dataUrl).get().build()

        // 워밍업 (캐시 채우기)
        client.newCall(request).execute().use { it.body.bytes() }

        // 캐시 히트 속도 측정
        val cacheStart = System.nanoTime()
        repeat(1000) {
            client.newCall(request).execute().use { it.body.bytes() }
        }
        val cacheNs = System.nanoTime() - cacheStart
        val cacheOpsPerSec = 1000.0 * 1_000_000_000L / cacheNs

        log.info { "캐시 히트: ${"%.0f".format(cacheOpsPerSec)} ops/s (단일 스레드)" }

        // 단일 스레드에서도 no-cache(100 ops/s @ 10ms) 대비 훨씬 빠름
        (cacheOpsPerSec > 1000.0).shouldBeTrue()
    }
}

private fun gzip(text: String): ByteArray {
    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
    return baos.toByteArray()
}
