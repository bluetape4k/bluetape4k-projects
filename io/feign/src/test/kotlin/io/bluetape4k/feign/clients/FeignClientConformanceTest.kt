package io.bluetape4k.feign.clients

import feign.AsyncClient
import feign.Client
import feign.Request
import feign.RequestTemplate
import feign.Response
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.bodyAsReader
import io.bluetape4k.feign.feignRequestOf
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.toUtf8String
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * 현재 지원하는 HC5와 Vert.x 동기 adapter의 전송 통합 계약을 검증합니다.
 *
 * core의 charset 및 header-template 처리가 실제 전송 경로에서도 유지되는지 확인합니다.
 * 빈 요청의 길이 0은 두 adapter의 현재 정책이며 모든 Feign Client의 불변 조건은 아닙니다.
 * 다른 adapter를 추가할 때는 정확한 wire header 기대값을 먼저 검토해야 합니다.
 */
abstract class FeignSyncClientConformanceTest: AbstractFeignTest() {

    private lateinit var server: MockWebServer
    private lateinit var client: Client

    protected abstract fun newClient(): Client

    @BeforeEach
    fun startServer() {
        server = MockWebServer().apply { start() }
        client = newClient()
    }

    @AfterEach
    fun stopServer() {
        (client as? AutoCloseable)?.closeSafe()
        server.closeSafe()
    }

    @Test
    fun `delayed response completes and body can be closed`() {
        server.enqueue(
            MockResponse()
                .setBody("delayed")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        )

        client.execute(request(), defaultOptions()).use { response ->
            response.status() shouldBeEqualTo 200
            response.body().asInputStream().readBytes().toUtf8String() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `read timeout completes with failure instead of hanging`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = assertFailsWith<Exception> {
            client.execute(request(), timeoutOptions())
        }
        error.shouldNotBeNull()
    }

    private fun request(): Request =
        feignRequestOf(server.url("/").toString())

    @ParameterizedTest
    @EnumSource(value = Request.HttpMethod::class, names = ["GET", "POST", "PUT", "PATCH"])
    fun `빈 요청은 HTTP method와 본문 없는 계약을 보존한다`(method: Request.HttpMethod) {
        for (body in listOf(null, byteArrayOf())) {
            server.enqueue(MockResponse())
            client.execute(
                feignRequestOf(server.url("/").toString(), method, body = body),
                defaultOptions()
            ).use { it.status() shouldBeEqualTo 200 }

            val recorded = server.takeRequest(2, TimeUnit.SECONDS).shouldNotBeNull()
            recorded.method shouldBeEqualTo method.name
            recorded.bodySize shouldBeEqualTo 0L
            // HC5는 빈 entity를 만들고, Vert.x는 빈 본문의 길이를 명시합니다.
            recorded.headers.values("Content-Length") shouldBeEqualTo listOf("0")
            recorded.getHeader("Content-Type").shouldBeNull()
            recorded.getHeader("Transfer-Encoding").shouldBeNull()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Content-Length", "content-length", "CoNtEnT-LeNgTh"])
    fun `UTF-8 요청의 바이트 길이를 중복 헤더 없이 전송한다`(headerName: String) {
        val expected = "한글 café 😀"
        val bytes = expected.toByteArray(Charsets.UTF_8)
        server.enqueue(MockResponse())
        client.execute(
            feignRequestOf(
                server.url("/").toString(),
                Request.HttpMethod.POST,
                headers = mapOf(headerName to listOf(bytes.size.toString())),
                body = bytes
            ),
            defaultOptions()
        ).use { it.status() shouldBeEqualTo 200 }

        val recorded = server.takeRequest(2, TimeUnit.SECONDS).shouldNotBeNull()
        recorded.headers.values("Content-Length") shouldBeEqualTo listOf(bytes.size.toString())
        recorded.bodySize shouldBeEqualTo bytes.size.toLong()
        recorded.body.readUtf8() shouldBeEqualTo expected
        recorded.getHeader("Transfer-Encoding").shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["\r", "\n", "\r\n"])
    fun `header template은 개행을 제거하고 별도 헤더를 주입하지 않는다`(newline: String) {
        val value = "safe" + newline + "X-Injected: evil"
        for (template in listOf(
            RequestTemplate().method(Request.HttpMethod.GET).header("X-Custom", value),
            RequestTemplate().method(Request.HttpMethod.GET).header("X-Custom", "{value}")
        )) {
            val resolved = template.resolve(mapOf("value" to value))
            resolved.headers()["X-Custom"]?.toList() shouldBeEqualTo listOf("safeX-Injected: evil")
            server.enqueue(MockResponse())
            client.execute(
                feignRequestOf(server.url("/").toString(), headers = resolved.headers()),
                defaultOptions()
            ).use { it.status() shouldBeEqualTo 200 }
            val recorded = server.takeRequest(2, TimeUnit.SECONDS).shouldNotBeNull()
            recorded.getHeader("X-Custom") shouldBeEqualTo "safeX-Injected: evil"
            recorded.getHeader("X-Injected").shouldBeNull()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["text/plain", "text/plain; charset=not-a-charset", "text/plain; charset=invalid charset"])
    fun `전송된 비ASCII 응답을 charset fallback으로 복원한다`(contentType: String) {
        val expected = "한글 응답 café 😀"
        server.enqueue(MockResponse().setHeader("Content-Type", contentType).setBody(expected))
        client.execute(request(), defaultOptions()).use { response ->
            response.bodyAsReader().use { it.readText() } shouldBeEqualTo expected
        }
    }

    @Test
    fun `음수 응답 길이는 정상 body로 반환하지 않고 전송 오류로 거부한다`() {
        server.enqueue(
            MockResponse().setBody("invalid")
                .setHeader("Content-Length", "-5")
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END)
        )

        val error = assertFailsWith<Exception> {
            client.execute(request(), defaultOptions()).use { response ->
                response.body().asInputStream().readBytes()
            }
        }
        // 동기 HC5의 IOException과 Vert.x future의 원인 예외를 구분하며 timeout은 허용하지 않습니다.
        val transportError = if (error is ExecutionException) error.cause else error
        (transportError is IOException || transportError is IllegalArgumentException)
            .shouldBeTrue()
        generateSequence(error as Throwable) { it.cause }
            .none { it is java.net.SocketTimeoutException || it is java.util.concurrent.TimeoutException }
            .shouldBeTrue()
        val messages = generateSequence(error as Throwable) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        messages shouldContain "content"
        messages shouldContain "length"
        messages shouldContain "-5"
    }
}

/**
 * Shared conformance tests for asynchronous Feign HTTP transport adapters.
 */
abstract class FeignAsyncClientConformanceTest<C: Any>: AbstractFeignTest() {

    private lateinit var server: MockWebServer
    private lateinit var client: AsyncClient<C>

    protected abstract fun newAsyncClient(): AsyncClient<C>

    protected abstract fun requestContext(): Optional<C>

    @BeforeEach
    fun startServer() {
        server = MockWebServer().apply { start() }
        client = newAsyncClient()
    }

    @AfterEach
    fun stopServer() {
        (client as? AutoCloseable)?.closeSafe()
        server.closeSafe()
    }

    @Test
    fun `delayed async response completes and body can be closed`() {
        server.enqueue(
            MockResponse()
                .setBody("delayed")
                .setBodyDelay(100, TimeUnit.MILLISECONDS)
        )

        executeAsync(defaultOptions()).get(2, TimeUnit.SECONDS).use { response ->
            response.status() shouldBeEqualTo 200
            response.body().asInputStream().readBytes().toUtf8String() shouldBeEqualTo "delayed"
        }
    }

    @Test
    fun `async read timeout completes exceptionally instead of hanging`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val error = assertFailsWith<ExecutionException> {
            executeAsync(timeoutOptions()).get(2, TimeUnit.SECONDS)
        }
        error.cause.shouldNotBeNull()
    }

    @Test
    fun `cancel marks async response future cancelled`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val future = executeAsync(timeoutOptions())

        future.cancel(true) shouldBeEqualTo true
        future.isCancelled shouldBeEqualTo true
    }

    private fun executeAsync(options: Request.Options): CompletableFuture<Response> =
        client.execute(
            feignRequestOf(server.url("/").toString()),
            options,
            requestContext()
        )
}

private fun defaultOptions(): Request.Options =
    Request.Options(1, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true)

private fun timeoutOptions(): Request.Options =
    Request.Options(100, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS, true)
