package io.bluetape4k.http.benchmark

import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.cache.InMemoryHttpCacheStorage
import io.bluetape4k.http.hc5.cache.cachingHttpAsyncClient
import io.bluetape4k.http.hc5.cache.cachingHttpClient
import io.bluetape4k.http.okhttp3.executeSuspending
import io.bluetape4k.http.okhttp3.okHttp3ConnectionPool
import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.http.okhttp3.okhttp3DispatcherWithVirtualThread
import io.bluetape4k.http.okhttp3.okhttp3RequestOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.cache.CacheConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 * HTTP 클라이언트 구현체별 성능 비교 벤치마크.
 *
 * 비교 대상:
 * - HC5 Classic + Platform Thread Pool
 * - HC5 Classic + Virtual Thread (병렬)
 * - HC5 Classic + InMemory Cache (캐시 히트)
 * - HC5 Async + Coroutines (병렬)
 * - HC5 Async + InMemory Cache + Coroutines (캐시 히트)
 * - OkHttp3 + Virtual Thread Dispatcher (병렬)
 * - OkHttp3 + Coroutines (병렬)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class HttpClientBenchmarkTest {

    companion object: KLogging() {
        private const val REQUESTS = 200
        private const val WARMUP = 20
        private const val MAX_CONNECTIONS = 50
        private const val RESPONSE_BODY = """{"id":1,"name":"bluetape4k","status":"ok"}"""
        private const val CACHE_MAX_AGE_SEC = 60
    }

    private lateinit var server: MockWebServer
    private lateinit var getUrl: String
    private lateinit var cacheUrl: String

    /** 큐 소진 없이 항상 200 응답 — no-cache */
    private val noCacheDispatcher = object: Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            MockResponse()
                .setResponseCode(200)
                .setBody(RESPONSE_BODY)
                .addHeader("Content-Type", "application/json")
    }

    /** 항상 200 응답 + Cache-Control: max-age=60 */
    private val cacheableDispatcher = object: Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            MockResponse()
                .setResponseCode(200)
                .setBody(RESPONSE_BODY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Cache-Control", "max-age=$CACHE_MAX_AGE_SEC, public")
    }

    @BeforeAll
    fun beforeAll() {
        server = MockWebServer()
        server.dispatcher = noCacheDispatcher
        server.start()
        val base = server.url("/").toString().trimEnd('/')
        getUrl  = "$base/get"
        cacheUrl = "$base/cached"
        log.info { "MockWebServer started: $base" }
    }

    @AfterAll
    fun afterAll() {
        runCatching { server.shutdown() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 측정 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private fun measure(label: String, block: () -> Unit): Long {
        repeat(WARMUP) { block() }
        val elapsed = measureTimeMillis { repeat(REQUESTS) { block() } }
        val ops = REQUESTS * 1000L / elapsed
        log.info { "[$label] sequential $REQUESTS req → ${elapsed}ms → $ops ops/s" }
        return ops
    }

    private fun measureParallel(label: String, block: suspend (Int) -> Unit): Long {
        runBlocking { repeat(WARMUP) { block(it) } }
        val elapsed = measureTimeMillis {
            runBlocking {
                (0 until REQUESTS).map { async { block(it) } }.awaitAll()
            }
        }
        val ops = REQUESTS * 1000L / elapsed
        log.info { "[$label] parallel $REQUESTS req → ${elapsed}ms → $ops ops/s" }
        return ops
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HC5 Classic + Platform Thread Pool (순차)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    fun `HC5 Classic + Platform Thread Pool (sequential)`() {
        server.dispatcher = noCacheDispatcher
        val cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(MAX_CONNECTIONS)
            .setMaxConnTotal(MAX_CONNECTIONS)
            .build()
        val client = HttpClients.custom().setConnectionManager(cm).build()

        val ops = measure("HC5-Classic/PlatformThread") {
            client.execute(ClassicRequestBuilder.get(getUrl).build()) { r ->
                EntityUtils.consume(r.entity); r.code
            }
        }

        client.close()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. HC5 Classic + Virtual Thread (병렬)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    fun `HC5 Classic + Virtual Thread (parallel)`() {
        server.dispatcher = noCacheDispatcher
        val cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(MAX_CONNECTIONS)
            .setMaxConnTotal(MAX_CONNECTIONS)
            .build()
        val client = HttpClients.custom().setConnectionManager(cm).build()
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        // 워밍업
        repeat(WARMUP) {
            client.execute(ClassicRequestBuilder.get(getUrl).build()) { r ->
                EntityUtils.consume(r.entity); r.code
            }
        }
        val elapsed = measureTimeMillis {
            (0 until REQUESTS)
                .map { executor.submit { client.execute(ClassicRequestBuilder.get(getUrl).build()) { r -> EntityUtils.consume(r.entity); r.code } } }
                .forEach { it.get() }
        }
        val ops = REQUESTS * 1000L / elapsed
        log.info { "[HC5-Classic/VirtualThread] parallel $REQUESTS req → ${elapsed}ms → $ops ops/s" }

        executor.shutdown(); client.close()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. HC5 Classic + InMemory Cache — 캐시 미스 vs 캐시 히트
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    fun `HC5 Classic + InMemory Cache (miss then hit)`() {
        server.dispatcher = cacheableDispatcher
        val storage = InMemoryHttpCacheStorage.createObjectCache(
            CacheConfig.custom().setMaxObjectSize(65536).build()
        )
        val client = cachingHttpClient(storage)

        // 첫 요청(미스)으로 캐시 채움
        val misOps = measure("HC5-Classic/Cache-MISS") {
            client.execute(ClassicRequestBuilder.get(cacheUrl).build()) { r ->
                EntityUtils.consume(r.entity); r.code
            }
        }

        // 이후 요청은 모두 캐시 히트 (서버 미도달)
        server.dispatcher = noCacheDispatcher  // 캐시 히트 구간에선 서버 도달 안 해야 함
        val hitOps = measure("HC5-Classic/Cache-HIT") {
            client.execute(ClassicRequestBuilder.get(cacheUrl).build()) { r ->
                EntityUtils.consume(r.entity); r.code
            }
        }

        log.info { "[Cache효과] miss=${misOps} ops/s → hit=${hitOps} ops/s (${hitOps / misOps.coerceAtLeast(1)}x)" }
        client.close()
        hitOps shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. HC5 Async + Coroutines (병렬)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    fun `HC5 Async + Coroutines (parallel)`() {
        server.dispatcher = noCacheDispatcher
        val cm = asyncClientConnectionManager {
            setMaxConnPerRoute(MAX_CONNECTIONS)
            setMaxConnTotal(MAX_CONNECTIONS)
        }
        val client = httpAsyncClient { setConnectionManager(cm) }

        val ops = measureParallel("HC5-Async/Coroutines") {
            val request = SimpleRequestBuilder.get(getUrl).build()
            val response = client.executeSuspending(request, HttpClientContext.create())
            response.code shouldBeGreaterThan 0
        }

        client.close(); cm.close()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. HC5 Async + InMemory Cache + Coroutines (캐시 히트)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    fun `HC5 Async + InMemory Cache + Coroutines (cache-hit)`() {
        server.dispatcher = cacheableDispatcher
        val client = cachingHttpAsyncClient {}.also { it.start() }

        // 워밍업: 캐시 채움
        runBlocking {
            repeat(WARMUP) {
                val request = SimpleRequestBuilder.get(cacheUrl).build()
                client.executeSuspending(SimpleRequestProducer.create(request), SimpleResponseConsumer.create())
            }
        }

        // 이후 모두 캐시 히트
        val elapsed = measureTimeMillis {
            runBlocking {
                (0 until REQUESTS).map {
                    async {
                        val request = SimpleRequestBuilder.get(cacheUrl).build()
                        client.executeSuspending(SimpleRequestProducer.create(request), SimpleResponseConsumer.create())
                    }
                }.awaitAll()
            }
        }
        val ops = REQUESTS * 1000L / elapsed
        log.info { "[HC5-Async/Cache-HIT/Coroutines] parallel $REQUESTS req → ${elapsed}ms → $ops ops/s" }

        client.close()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. OkHttp3 + Virtual Thread Dispatcher (병렬)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    fun `OkHttp3 + Virtual Thread Dispatcher (parallel)`() {
        server.dispatcher = noCacheDispatcher
        val client = okhttp3Client(
            connectionPool = okHttp3ConnectionPool(maxIdleConnections = MAX_CONNECTIONS),
            dispatcher = okhttp3DispatcherWithVirtualThread()
        )
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        repeat(WARMUP) {
            client.newCall(okhttp3RequestOf(getUrl)).execute().use { it.body.bytes() }
        }
        val elapsed = measureTimeMillis {
            (0 until REQUESTS)
                .map { executor.submit { client.newCall(okhttp3RequestOf(getUrl)).execute().use { r -> r.body.bytes() } } }
                .forEach { it.get() }
        }
        val ops = REQUESTS * 1000L / elapsed
        log.info { "[OkHttp3/VirtualThread] parallel $REQUESTS req → ${elapsed}ms → $ops ops/s" }

        executor.shutdown()
        client.dispatcher.executorService.shutdown()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. OkHttp3 + Coroutines (병렬)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    fun `OkHttp3 + Coroutines (parallel)`() {
        server.dispatcher = noCacheDispatcher
        val client = okhttp3Client(
            connectionPool = okHttp3ConnectionPool(maxIdleConnections = MAX_CONNECTIONS),
            dispatcher = okhttp3DispatcherWithVirtualThread()
        )

        val ops = measureParallel("OkHttp3/Coroutines") {
            client.executeSuspending(okhttp3RequestOf(getUrl)).use { it.body.bytes() }
        }

        client.dispatcher.executorService.shutdown()
        ops shouldBeGreaterThan 0L
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. 요약
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(99)
    fun `종합 결과 요약`() {
        log.info { "" }
        log.info { "=====================================================" }
        log.info { " HTTP Client Benchmark ($REQUESTS requests, warmup=$WARMUP)" }
        log.info { " 1. HC5 Classic/PlatformThread — sequential" }
        log.info { " 2. HC5 Classic/VirtualThread  — parallel" }
        log.info { " 3. HC5 Classic/Cache          — miss vs hit" }
        log.info { " 4. HC5 Async/Coroutines       — parallel" }
        log.info { " 5. HC5 Async/Cache/Coroutines — parallel cache-hit" }
        log.info { " 6. OkHttp3/VirtualThread      — parallel" }
        log.info { " 7. OkHttp3/Coroutines         — parallel" }
        log.info { "=====================================================" }
        true.shouldBeTrue()
    }
}
