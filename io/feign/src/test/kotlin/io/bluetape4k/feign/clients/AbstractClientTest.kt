package io.bluetape4k.feign.clients

import com.fasterxml.jackson.databind.json.JsonMapper
import feign.CollectionFormat
import feign.FeignException
import feign.Headers
import feign.Logger
import feign.Param
import feign.RequestLine
import feign.Response
import feign.hc5.ApacheHttp5Client
import feign.jaxrs2.JAXRS2Contract
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.client
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.toUtf8String
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeNullOrBlank
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

abstract class AbstractClientTest: AbstractFeignTest() {

    companion object: KLogging() {
        @JvmStatic
        protected val mapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }

    protected abstract fun newBuilder(): feign.Feign.Builder

    private lateinit var server: MockWebServer
    private lateinit var api: TestInterface

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer()
        api = newBuilder().client(server.baseUrl)
    }

    @AfterEach
    fun afterEach() {
        server.closeSafe()
    }

    @Test
    fun `run patch method`() {
        server.enqueue(MockResponse().setBody("foo"))
        server.enqueue(MockResponse())

        api.patch("") shouldBeEqualTo "foo"

        val request = server.takeRequest()
        request.headers["Accept"] shouldBeEqualTo "text/plain"
        request.headers["Content-Length"] shouldBeEqualTo "0"
        request.headers["Content-Type"].shouldBeNull()
        request.method shouldBeEqualTo "PATCH"
    }


    @Test
    fun `parse request and response`() {
        server.enqueue(MockResponse().setBody("foo").addHeader("Foo: Bar"))

        val response = api.post("foo")

        log.debug { "response=$response" }
        response.status() shouldBeEqualTo 200
        response.reason() shouldBeEqualTo "OK"
        response.headers()["Content-Length"]!! shouldContain "3"
        response.headers()["Foo"]!! shouldContain "Bar"
        response.body().asInputStream().readBytes().toUtf8String() shouldBeEqualTo "foo"

        val request = server.takeRequest()
        log.debug { "request=$request" }
        request.method shouldBeEqualTo "POST"
        request.headers["Foo"] shouldBeEqualTo "Bar, Baz"
        request.headers["Accept"] shouldBeEqualTo "*/*"
        request.headers["Content-Length"] shouldBeEqualTo "3"
        request.body.readUtf8() shouldBeEqualTo "foo"
    }

    @Test
    fun `reason phrase is optional`() {
        server.enqueue(MockResponse().setStatus("HTTP/1.1 " + 200))

        val response = api.post("foo")

        log.debug { "response=$response" }
        response.status() shouldBeEqualTo 200
        response.reason().shouldBeNullOrBlank()
    }

    @Test
    fun `parse error response`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("ARGHH"))

        assertFailsWith<FeignException> {
            api.get()
        }
            .message shouldBeEqualTo
                "[500 Server Error] during [GET] to [${server.url("/")}] [TestInterface#get()]: [ARGHH]"
    }

    @Test
    fun `parse error response body`() {
        val expectedResponseBody = "ARGHH"
        server.enqueue(MockResponse().setResponseCode(500).setBody(expectedResponseBody))

        assertFailsWith<FeignException> {
            api.get()
        }.contentUTF8() shouldBeEqualTo expectedResponseBody
    }

    @Test
    fun `parse un authorized response body`() {
        val expectedResponseBody = "ARGHH"
        server.enqueue(MockResponse().setResponseCode(401).setBody(expectedResponseBody))

        assertFailsWith<FeignException> {
            api.postForString("HELLO")
        }.contentUTF8() shouldBeEqualTo expectedResponseBody
    }


    /**
     * This shows that is a no-op or otherwise doesn't cause an NPE when there's no content.
     *
     * [204 No Content](https://developer.mozilla.org/ko/docs/Web/HTTP/Status/204)
     */
    @Test
    fun `safe rebuffering`() {
        // NO CONTENT -> 클라가 캐시에서 가져오도록 한다
        server.enqueue(MockResponse().setResponseCode(204))
        api.post("foo")
    }

    @Test
    fun `non response body for POST`() {
        server.enqueue(MockResponse())
        api.noPostBody()
    }

    @Test
    fun `non response body for PUT`() {
        server.enqueue(MockResponse())
        api.noPutBody()
    }

    @Test
    fun `non response body for PATCH`() {
        server.enqueue(MockResponse())
        api.noPatchBody()
    }

    @Test
    fun `parse response missing length`() {
        server.enqueue(MockResponse().setChunkedBody("foo", 1))

        val response = api.post("testing")

        response.status() shouldBeEqualTo 200
        response.reason() shouldBeEqualTo "OK"

        // NOTE: LogLevel 을 FULL 로 하면 미리 다 가져오기 때문에 length 에 값을 가지게 된다.
        // response.body().length().shouldBeNull()
        response.body().asInputStream().toUtf8String() shouldBeEqualTo "foo"
    }

    @Test
    open fun `very long response null length`() {
        server.enqueue(MockResponse().setBody("AAAAAAAA").addHeader("Content-Length", Long.MAX_VALUE))

        val response = api.post("foo")
        log.debug { "response=$response" }

        // NOTE: LogLevel 을 FULL 로 하면 미리 다 가져오기 때문에 length 에 값을 가지게 된다.
        // response.body().length().shouldBeNull()
    }

    @Test
    fun `response length not provided`() {
        server.enqueue(MockResponse().setBody("test"))

        val response = api.post("")
        response.body().length() shouldBeEqualTo 4
    }

    @Test
    fun `contentType with charset`() {
        val expected = "AAAAAAA"
        server.enqueue(MockResponse().setBody(expected))

        val response = api.postWithContentType("foo", "text/plain; charset=utf-8")
        response.body().asInputStream().toUtf8String() shouldBeEqualTo expected
    }

    @EnabledOnOs(OS.MAC)
    @Test
    fun `content type defaults to request charset`() {
        server.enqueue(MockResponse().setBody("foo"))

        val expectedBody = "안녕하세요-àáâãäåèéêë"
        api.postWithContentType(expectedBody, "text/plain; charset=utf-8")

        server.takeRequest().body.readUtf8() shouldBeEqualTo expectedBody
    }

    @Test
    fun `default collection format`() {
        server.enqueue(MockResponse().setBody("body"))

        val response = api.get(listOf("bar", "baz"))

        response.status() shouldBeEqualTo 200
        response.reason() shouldBeEqualTo "OK"

        server.takeRequest().path shouldBeEqualTo "/?foo=bar&foo=baz"
    }

    @Test
    fun `headers with null params`() {
        server.enqueue(MockResponse().setBody("body"))

        val response = api.getWithHeaders(null)

        response.status() shouldBeEqualTo 200
        response.reason() shouldBeEqualTo "OK"

        with(server.takeRequest()) {
            method shouldBeEqualTo "GET"
            path shouldBeEqualTo "/"
            headers["Authorization"].shouldBeNull()
        }
    }

    @Test
    fun `alternative collection format - CSV`() {
        server.enqueue(MockResponse().setBody("body"))

        val response = api.getCSV(listOf("bar", "baz"))

        response.status() shouldBeEqualTo 200
        response.reason() shouldBeEqualTo "OK"

        with(server.takeRequest()) {
            method shouldBeEqualTo "GET"
            path shouldBeIn listOf("/?foo=bar,baz", "/?foo=bar%2Cbaz")
        }
    }

    @Test
    fun `can support Gzip`() {
        val responseData = "Compressed Data"
        server.enqueue(
            MockResponse()
                .addHeader("Content-Encoding", "gzip")
                .setBody(Buffer().write(gzip(responseData)))
        )

        val result = api.get()
        result.shouldNotBeNull() shouldBeEqualTo responseData
    }

    @Test
    fun `can support Deflate`() {
        val responseData = "Compressed Data"
        server.enqueue(
            MockResponse()
                .addHeader("Content-Encoding", "deflate")
                .setBody(Buffer().write(deflate(responseData)))
        )

        val result = api.get()
        result.shouldNotBeNull() shouldBeEqualTo responseData
    }

    @Test
    fun `can except case insensitive header`() {
        val responseData = "Compressed Data"
        server.enqueue(
            MockResponse()
                .addHeader("content-encoding", "gzip")
                .setBody(Buffer().write(gzip(responseData)))
        )

        val result = api.get()
        result shouldBeEqualTo responseData
    }

    interface TestInterface {
        @RequestLine("POST /?foo=bar&foo=baz&qux=")
        @Headers("Foo: Bar", "Foo: Baz", "Qux: ", "Content-Type: text/plain")
        fun post(body: String): Response

        @RequestLine("POST /path/{to}/resource")
        @Headers("Accept: text/plain")
        fun post(@Param("to") to: String?, body: String?): Response?

        @RequestLine("POST /?foo=bar&foo=baz&qux=")
        @Headers("Foo: Bar", "Foo: Baz", "Qux: ", "Content-Type: text/plain")
        fun postForString(body: String?): String?

        @RequestLine("GET /")
        @Headers("Accept: text/plain", "Accept-Encoding: gzip, deflate, lz4, snappy, zstd")
        fun get(): String?

        @RequestLine("GET /?foo={multiFoo}")
        fun get(@Param("multiFoo") multiFoo: List<String?>?): Response

        @Headers("Authorization: {authorization}")
        @RequestLine("GET /")
        fun getWithHeaders(@Param("authorization") authorization: String?): Response

        @RequestLine(value = "GET /?foo={multiFoo}", collectionFormat = CollectionFormat.CSV)
        fun getCSV(@Param("multiFoo") multiFoo: List<String?>?): Response

        @RequestLine("PATCH /")
        @Headers("Accept: text/plain")
        fun patch(body: String?): String?

        @RequestLine("POST")
        fun noPostBody(): String

        @RequestLine("PUT")
        fun noPutBody(): String

        @RequestLine("PATCH")
        fun noPatchBody(): String

        @RequestLine("POST /?foo=bar&foo=baz&qux=")
        @Headers("Foo: Bar", "Foo: Baz", "Qux: ", "Content-Type: {contentType}")
        fun postWithContentType(body: String?, @Param("contentType") contentType: String?): Response
    }

    private fun gzip(data: String): ByteArray {
        return Compressors.GZip.compress(data.toUtf8Bytes())
    }

    private fun deflate(data: String): ByteArray {
        return Compressors.Deflate.compress(data.toUtf8Bytes())
    }

    /**
     * Quarkus 처럼 JAX-RS 표준 Annotation을 사용하여 Feign Client를 구현할 경우
     */
    @Test
    fun `jax-rs2 query params are respected when body is empty`() {
        val httpClient = HttpClientBuilder.create().build()
        val jaxRsTestIntf = feignBuilder {
            this.contract(JAXRS2Contract())
            client(ApacheHttp5Client(httpClient))
            encoder(JacksonEncoder2(mapper))
            decoder(JacksonDecoder2(mapper))  // text/plain 을 받을 때는 예외가 발생한다. 이를 어찌 해결해야 하나? -> ResponseMapper 를 이용하면 된다.
            logger(Slf4jLogger(JaxRsTestInterface::class.java))
            logLevel(Logger.Level.FULL)
        }
            .client<JaxRsTestInterface>(server.baseUrl)

        val user = User("debop", 54)
        val userJson = mapper.writeValueAsString(user)

        server.enqueueBody(userJson, "content-type: application/json")
        server.enqueueBody(userJson, "Content-Type: application/json")

        // 두번째 인자가 request body가 된다

        jaxRsTestIntf.withBody("bar", user) shouldBeEqualTo user
        val request1 = server.takeRequest()
        request1.path shouldBeEqualTo "/withBody?foo=bar"
        request1.body.readUtf8() shouldBeEqualTo mapper.writeValueAsString(user)

        // RequestBody 값이 제공되지 않았으므로 null 이다.
        jaxRsTestIntf.withoutBody("bar") shouldBeEqualTo user
        val request2 = server.takeRequest()
        request2.path shouldBeEqualTo "/withoutBody?foo=bar"
        request2.body.readUtf8() shouldBeEqualTo ""
    }

    @Path("/")
    interface JaxRsTestInterface {
        @PUT
        @Path("/withBody")
        fun withBody(@QueryParam("foo") foo: String, user: User): User?

        @PUT
        @Path("/withoutBody")
        fun withoutBody(@QueryParam("foo") foo: String): User?
    }

    data class User(
        val name: String,
        val age: Int,
    )
}
