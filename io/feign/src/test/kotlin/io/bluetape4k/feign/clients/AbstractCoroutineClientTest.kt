package io.bluetape4k.feign.clients

import com.fasterxml.jackson.databind.json.JsonMapper
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.codec.Decoder
import feign.kotlin.CoroutineFeign
import io.bluetape4k.feign.codec.FeignFastjsonDecoder
import io.bluetape4k.feign.codec.FeignFastjsonEncoder
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.coroutines.client
import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.http.okhttp3.mock.enqueueBodyWithDelay
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.closeSafe
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.time.Duration

abstract class AbstractCoroutineClientTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        protected val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
        private val delay = Duration.ofMillis(10)
    }

    private lateinit var server: MockWebServer
    private lateinit var api: TestInterfaceAsync

    protected abstract fun newCoroutineBuilder(): CoroutineFeign.CoroutineBuilder<*>

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer()
        api = newCoroutineBuilder().client(server.baseUrl)
    }

    @AfterEach
    fun afterEach() {
        server.closeSafe()
    }

    @Test
    fun `응답 수형이 기본 수형이면 sut가 수행되어야 한다`() = runSuspendTest {
        val expected = "Hello World"
        server.enqueueBodyWithDelay(expected, delay, "Content-Type: text/plain; charset=utf-8")

        val client = newCoroutineBuilder()
            .decoder(Decoder.Default()) // 기본 수형을 받기 위해 (JSON 이 아닌 일반 문자열)
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderThatReturningBasicType(1)
        result shouldBeEqualTo expected
    }

    @Test
    fun `응답이 클래스라면 JSON 방식으로 Jackson 디코딩 되어야 합니다`() = runSuspendTest {
        val expected = IceCreamOrder("HELLO WORLD", 999)
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(JacksonEncoder2.INSTANCE) // Jackson 을 사용하기 위해
            .decoder(JacksonDecoder2.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderThatReturningComplexType(1)

        result shouldBeEqualTo expected
    }

    @Test
    fun `응답이 클래스라면 JSON 방식으로 Fastjson2 디코딩 되어야 합니다`() = runSuspendTest {
        val expected = IceCreamOrder("HELLO WORLD", 999)
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(FeignFastjsonEncoder.INSTANCE) // Fastjson2 을 사용하기 위해
            .decoder(FeignFastjsonDecoder.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderThatReturningComplexType(1)

        result shouldBeEqualTo expected
    }


    @Test
    fun `응답이 엔티티의 컬렉션이라면 Jackson JSON 방식으로 디코딩 되어야 합니다`() = runSuspendTest {
        val expected = listOf(
            IceCreamOrder("HELLO WORLD", 999),
            IceCreamOrder("베스킨 라빈스", 31)
        )
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(JacksonEncoder2.INSTANCE) // Jackson 을 사용하기 위해
            .decoder(JacksonDecoder2.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrders()

        result shouldContainSame expected
    }

    @Test
    fun `응답이 엔티티의 컬렉션이라면 Fast JSON 방식으로 디코딩 되어야 합니다`() = runSuspendTest {
        val expected = listOf(
            IceCreamOrder("HELLO WORLD", 999),
            IceCreamOrder("베스킨 라빈스", 31)
        )
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(FeignFastjsonEncoder.INSTANCE) // Fastjson2 을 사용하기 위해
            .decoder(FeignFastjsonDecoder.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrders()

        result shouldContainSame expected
    }

    @Test
    fun `응답 수형이 Void 인 경우, 응답을 받아야 합니다`() = runSuspendTest {
        val expected = "Hello World"
        server.enqueueBody(expected)

        val client = newCoroutineBuilder().client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderThatReturningVoid(1)
        result.shouldBeNull()
    }

    @Test
    fun `응답 수형이 Unit 인 경우에도 응답을 받아야 합니다`() = runSuspendTest {
        val expected = "Hello World"
        server.enqueueBody(expected)

        val client = newCoroutineBuilder().client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderThatReturningUnit(1)
        result shouldBeEqualTo Unit
    }

    @Test
    fun `RequestBody 가 JSON 일 경우, 서버에서 받아야 합니다 with Jackson`() = runSuspendTest {
        val expected = IceCreamOrder("HELLO WORLD", 999)
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(JacksonEncoder2.INSTANCE) // Jackson 을 사용하기 위해
            .decoder(JacksonDecoder2.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderWithHttpBody(expected)
        result shouldBeEqualTo expected

        server.takeRequest().body.readUtf8() shouldBeEqualTo mapper.writeAsString(expected)
    }

    @Test
    fun `RequestBody 가 JSON 일 경우, 서버에서 받아야 합니다 with Fastjson2`() = runSuspendTest {
        val expected = IceCreamOrder("HELLO WORLD", 999)
        server.enqueueBodyWithDelay(
            mapper.writeAsString(expected),
            delay,
            "Content-Type: application/json; charset=UTF-8"
        )

        val client = newCoroutineBuilder()
            .encoder(FeignFastjsonEncoder.INSTANCE) // Fastjson 을 사용하기 위해
            .decoder(FeignFastjsonDecoder.INSTANCE) // JSON 을 받기 위해
            .client<TestInterfaceAsync>(server.baseUrl)

        val result = client.findOrderWithHttpBody(expected)
        result shouldBeEqualTo expected

        server.takeRequest().body.readUtf8() shouldBeEqualTo mapper.writeAsString(expected)
    }

    internal interface TestInterfaceAsync {
        @RequestLine("GET /icecream/orders/{orderId}")
        suspend fun findOrderThatReturningBasicType(@Param("orderId") orderId: Int): String

        @RequestLine("GET /icecream/orders/{orderId}")
        @Headers("Content-Type: application/json; charset=UTF-8")
        suspend fun findOrderThatReturningComplexType(@Param("orderId") orderId: Int): IceCreamOrder

        @RequestLine("GET /icecream/orders/{orderId}")
        suspend fun findOrderThatReturningVoid(@Param("orderId") orderId: Int): Void

        @RequestLine("GET /icecream/orders/{orderId}")
        suspend fun findOrderThatReturningUnit(@Param("orderId") orderId: Int)

        // 첫번째 인자는 자동으로 RequestBody 로 취급한다
        @RequestLine("POST /icecream/orders")
        @Headers("Content-Type: application/json; charset=UTF-8")
        suspend fun findOrderWithHttpBody(order: IceCreamOrder): IceCreamOrder

        @RequestLine("GET /icecream/orders")
        @Headers("Content-Type: application/json; charset=UTF-8")
        suspend fun findOrders(): List<IceCreamOrder>
    }

    data class IceCreamOrder(
        val id: String,
        val no: Long,
    ): Serializable
}
