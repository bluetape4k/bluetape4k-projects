package io.bluetape4k.feign.coroutines

import feign.Param
import feign.RequestLine
import feign.hc5.ApacheHttp5Client
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.closeSafe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable

/**
 * [coroutineFeignBuilder] 및 [coroutineFeignBuilderOf] 함수를 검증합니다.
 */
class FeignCoroutineBuilderSupportTest: AbstractFeignTest() {

    companion object: KLoggingChannel() {
        private val mapper = Jackson.defaultJsonMapper
    }

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterEach
    fun tearDown() {
        server.closeSafe()
    }

    interface SampleApi {
        @RequestLine("GET /items/{id}")
        suspend fun getItem(@Param("id") id: Int): SampleItem

        @RequestLine("GET /items")
        suspend fun listItems(): List<SampleItem>
    }

    data class SampleItem(val id: Int, val name: String) : Serializable

    @Test
    fun `coroutineFeignBuilder creates a valid builder`() {
        val builder = coroutineFeignBuilder<Any> { }

        builder.shouldNotBeNull()
    }

    @Test
    fun `coroutineFeignBuilderOf creates builder with default settings`() {
        val builder = coroutineFeignBuilderOf<Any>()

        builder.shouldNotBeNull()
    }

    @Test
    fun `coroutine feign client can call suspend API and decode JSON response`() = runSuspendTest {
        val expected = SampleItem(42, "Test Item")
        server.enqueueBody(
            mapper.writeAsString(expected).orEmpty(),
            "Content-Type: application/json; charset=UTF-8"
        )

        val api: SampleApi = coroutineFeignBuilderOf<Any>()
            .encoder(JacksonEncoder2.INSTANCE)
            .decoder(JacksonDecoder2.INSTANCE)
            .client(server.baseUrl)

        val result = api.getItem(42)

        result shouldBeEqualTo expected
    }

    @Test
    fun `coroutine feign client can decode list response`() = runSuspendTest {
        val expected = listOf(SampleItem(1, "Item A"), SampleItem(2, "Item B"))
        server.enqueueBody(
            mapper.writeAsString(expected).orEmpty(),
            "Content-Type: application/json; charset=UTF-8"
        )

        val api: SampleApi = coroutineFeignBuilderOf<Any>()
            .encoder(JacksonEncoder2.INSTANCE)
            .decoder(JacksonDecoder2.INSTANCE)
            .client(server.baseUrl)

        val result = api.listItems()

        result shouldBeEqualTo expected
    }

    @Test
    fun `coroutineFeignBuilderOf with ApacheHttp5Client creates valid client`() {
        val builder = coroutineFeignBuilderOf<Any>(
            asyncClient = feign.AsyncClient.Default(ApacheHttp5Client(), io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor),
        )

        builder.shouldNotBeNull()
    }

    @Test
    fun `client extension with baseUrl creates typed API`() = runSuspendTest {
        val expected = SampleItem(1, "Hello")
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mapper.writeAsString(expected).orEmpty())
        )

        val api: SampleApi = coroutineFeignBuilderOf<Any>()
            .encoder(JacksonEncoder2.INSTANCE)
            .decoder(JacksonDecoder2.INSTANCE)
            .client(server.baseUrl)

        val result = api.getItem(1)
        result.id shouldBeEqualTo 1
        result.name shouldBeEqualTo "Hello"
    }

    @Test
    fun `client extension with null baseUrl uses EmptyTarget`() {
        // Should not throw — EmptyTarget is used for dynamic URL APIs
        val builder = coroutineFeignBuilderOf<Any>()
            .encoder(JacksonEncoder2.INSTANCE)
            .decoder(JacksonDecoder2.INSTANCE)
        val api: SampleApi = builder.client(null as String?)

        api.shouldNotBeNull()
    }
}
