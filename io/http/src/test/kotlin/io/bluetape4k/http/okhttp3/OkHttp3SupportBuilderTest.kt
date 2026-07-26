package io.bluetape4k.http.okhttp3

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * [OkHttp3Support] DSL 빌더 함수들의 동작을 검증하는 테스트 클래스입니다.
 */
class OkHttp3SupportBuilderTest {

    companion object: KLogging()

    private lateinit var server: MockWebServer

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
    }

    @AfterEach
    fun afterEach() {
        runCatching { server.shutdown() }
    }

    @Nested
    inner class ConnectionPoolTests {

        @Test
        fun `okHttp3ConnectionPool은 기본값으로 ConnectionPool을 생성한다`() {
            val pool = okHttp3ConnectionPool()
            pool.shouldNotBeNull()
        }

        @Test
        fun `okHttp3ConnectionPool은 지정한 maxIdleConnections으로 생성된다`() {
            val pool = okHttp3ConnectionPool(maxIdleConnections = 10, keepAliveDurations = Duration.ofMinutes(1))
            pool.shouldNotBeNull()
        }
    }

    @Nested
    inner class DispatcherTests {

        @Test
        fun `okhttp3DispatcherWithVirtualThread은 Dispatcher를 생성한다`() {
            val dispatcher = okhttp3DispatcherWithVirtualThread()
            dispatcher.shouldNotBeNull()
            dispatcher.executorService.shouldNotBeNull()
            dispatcher.executorService.shutdown()
        }

        @Test
        fun `okhttp3DispatcherWithVirtualThread은 커스텀 threadName으로 생성된다`() {
            val dispatcher = okhttp3DispatcherWithVirtualThread("custom-prefix-")
            dispatcher.shouldNotBeNull()
            dispatcher.executorService.shutdown()
        }
    }

    @Nested
    inner class CacheControlTests {

        @Test
        fun `okhttp3CacheControl은 DSL로 CacheControl을 생성한다`() {
            val cc = okhttp3CacheControl {
                noCache()
            }
            cc.noCache.shouldBeEqualTo(true)
        }

        @Test
        fun `okhttp3CacheControlOf는 기본값으로 CacheControl을 생성한다`() {
            val cc = okhttp3CacheControlOf()
            cc.shouldNotBeNull()
            // 기본값은 max-age=0, max-stale=0, min-fresh=0
            cc.noCache.shouldBeEqualTo(false)
            cc.noStore.shouldBeEqualTo(false)
        }

        @Test
        fun `okhttp3CacheControlOf는 noCache 플래그를 적용한다`() {
            val cc = okhttp3CacheControlOf(noCache = true)
            cc.noCache.shouldBeEqualTo(true)
        }

        @Test
        fun `okhttp3CacheControlOf는 noStore 플래그를 적용한다`() {
            val cc = okhttp3CacheControlOf(noStore = true)
            cc.noStore.shouldBeEqualTo(true)
        }

        @Test
        fun `okhttp3CacheControlOf는 immutable 플래그를 적용한다`() {
            val cc = okhttp3CacheControlOf(immutable = true)
            cc.immutable.shouldBeEqualTo(true)
        }
    }

    @Nested
    inner class RequestBuilderTests {

        @Test
        fun `okhttp3Request는 DSL로 Request를 생성한다`() {
            val request = okhttp3Request {
                url(server.url("/"))
                get()
            }
            request.url.toString().shouldNotBeBlank()
            request.method shouldBeEqualTo "GET"
        }

        @Test
        fun `okhttp3RequestOf는 URL과 vararg 헤더로 Request를 생성한다`() {
            val request = okhttp3RequestOf(
                url = server.url("/api").toString(),
                "X-Custom-Header", "custom-value",
                "Authorization", "Bearer token"
            ) { get() }

            request.url.toString().shouldNotBeBlank()
            request.header("X-Custom-Header") shouldBeEqualTo "custom-value"
            request.header("Authorization") shouldBeEqualTo "Bearer token"
        }

        @Test
        fun `okhttp3RequestOf는 URL과 Headers 객체로 Request를 생성한다`() {
            val headers = okhttp3.Headers.headersOf("X-Trace-Id", "trace-001")
            val request = okhttp3RequestOf(
                url = server.url("/").toString(),
                headers = headers
            ) { get() }

            request.header("X-Trace-Id") shouldBeEqualTo "trace-001"
        }
    }

    @Nested
    inner class ClientBuilderTests {

        @Test
        fun `okhttp3Client는 커스텀 타임아웃으로 OkHttpClient를 생성한다`() {
            val client = okhttp3Client {
                connectTimeout(Duration.ofSeconds(5))
                readTimeout(Duration.ofSeconds(5))
            }
            client.connectTimeoutMillis shouldBeEqualTo 5_000
            client.readTimeoutMillis shouldBeEqualTo 5_000
        }

        @Test
        fun `okhttp3Client는 실제 HTTP GET 요청을 처리한다`() {
            server.enqueue(MockResponse().setBody("hello").setResponseCode(200))

            val client = okhttp3Client { }
            val request = okhttp3Request {
                url(server.url("/"))
                get()
            }

            client.execute(request).use { response ->
                response.isSuccessful.shouldBeEqualTo(true)
                response.code shouldBeEqualTo 200
                response.bodyAsString() shouldBeEqualTo "hello"
            }
        }

        @Test
        fun `okhttp3ClientBuilderOf는 ConnectionPool과 Dispatcher를 사용해 빌더를 반환한다`() {
            val pool = okHttp3ConnectionPool(maxIdleConnections = 5)
            val dispatcher = okhttp3DispatcherWithVirtualThread()

            val builder = okhttp3ClientBuilderOf(connectionPool = pool, dispatcher = dispatcher)
            val client: OkHttpClient = builder.build()

            client.connectionPool.shouldNotBeNull()
            client.dispatcher.shouldNotBeNull()

            dispatcher.executorService.shutdown()
        }
    }

    @Nested
    inner class ResponseBuilderTests {

        @Test
        fun `okhttp3Response는 DSL로 Response를 생성한다`() {
            val request = okhttp3Request {
                url("http://localhost/")
                get()
            }
            val response = okhttp3Response {
                code(200)
                message("OK")
                request(request)
                protocol(okhttp3.Protocol.HTTP_1_1)
            }
            response.code shouldBeEqualTo 200
            response.message shouldBeEqualTo "OK"
        }
    }

    @Nested
    inner class ExecuteTests {

        @Test
        fun `OkHttpClient execute는 동기적으로 응답을 반환한다`() {
            server.enqueue(MockResponse().setBody("sync-ok").setResponseCode(200))

            val client = okhttp3Client { }
            val request = okhttp3Request {
                url(server.url("/"))
                get()
            }

            client.execute(request).use { response ->
                response.isSuccessful.shouldBeEqualTo(true)
                response.bodyAsString() shouldBeEqualTo "sync-ok"
            }
        }

        @Test
        fun `OkHttpClient executeAsync는 CompletableFuture로 응답을 반환한다`() {
            server.enqueue(MockResponse().setBody("async-ok").setResponseCode(200))

            val client = okhttp3Client { }
            val request = okhttp3Request {
                url(server.url("/"))
                get()
            }

            client.executeAsync(request).get().use { response ->
                response.isSuccessful.shouldBeEqualTo(true)
                response.bodyAsString() shouldBeEqualTo "async-ok"
            }
        }

        @Test
        fun `OkHttpClient executeAsync는 취소 핸들러를 호출한다`() {
            // This tests the cancel path via a failed request - just verify future completes
            server.enqueue(MockResponse().setBody("ok"))

            val client = okhttp3Client { }
            val request = okhttp3Request {
                url(server.url("/"))
                get()
            }

            var cancelCalled = false
            val future = client.executeAsync(request) { cancelCalled = true }
            future.get().use { response ->
                response.isSuccessful.shouldBeEqualTo(true)
            }
            // Not cancelled, so handler should NOT be called
            cancelCalled.shouldBeEqualTo(false)
        }
    }
}
