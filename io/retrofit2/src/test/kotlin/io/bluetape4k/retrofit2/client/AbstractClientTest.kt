package io.bluetape4k.retrofit2.client

import io.bluetape4k.http.okhttp3.mock.baseUrl
import io.bluetape4k.http.okhttp3.mock.enqueueBody
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.retrofit2.AbstractRetrofitTest
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.TestService
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import retrofit2.converter.scalars.ScalarsConverterFactory

abstract class AbstractClientTest: AbstractRetrofitTest() {

    companion object: KLogging()

    protected abstract val callFactory: okhttp3.Call.Factory

    private lateinit var server: MockWebServer
    private lateinit var service: TestService.EmptyService
    private lateinit var api: TestService.TestInterface

    @BeforeEach
    fun beforeEach() {
        server = MockWebServer().apply { start() }
        server.start()
        service = retrofitOf(server.baseUrl, callFactory).service()
        api = retrofitOf(server.baseUrl, callFactory, ScalarsConverterFactory.create()).service()
    }

    @AfterEach
    fun afterEach() {
        server.shutdown()
    }

    @Test
    fun `verify api instance`() {
        service.shouldNotBeNull()
        api.shouldNotBeNull()
    }

    @Test
    fun `service get Unit`() {
        server.enqueueBody("")

        log.trace { "request=${service.empty().request()}" }

        val response = service.empty().execute()
        response.isSuccessful.shouldBeTrue()
        response.body() shouldBeEqualTo Unit
    }

    @Test
    fun `run patch method`() {
        server.enqueueBody("foo", "content-type: text/plain; charset=utf-8")
        api.patch().execute().body() shouldBeEqualTo "foo"

        val request = server.takeRequest()
        request.headers["Accept"] shouldBeEqualTo "text/plain"
        request.method shouldBeEqualTo "PATCH"
    }

    @Test
    fun `parse request and response`() {
        server.enqueueBody("foo", "Foo: Bar")

        val response = api.post("foo").execute()

        log.trace { "response=$response" }
        response.code() shouldBeEqualTo 200
        response.message() shouldBeEqualTo "OK"
        response.headers()["Content-Length"]!! shouldContain "3"
        response.headers()["Foo"]!! shouldContain "Bar"
        response.body() shouldBeEqualTo "foo"

        val request = server.takeRequest()
        log.debug { "request=$request" }
        request.method shouldBeEqualTo "POST"
        request.headers["Foo"]!! shouldBeEqualTo "Bar"
        // request.headers["Accept"] shouldBeEqualTo "*/*"
        request.headers["Content-Length"] shouldBeEqualTo "3"
        request.body.readUtf8() shouldBeEqualTo "foo"
    }

    @Test
    fun `reason phrase is optional`() {
        server.enqueue(MockResponse().setStatus("HTTP/1.1 " + 200))

        val response = api.post("foo").execute()

        log.trace { "response=$response" }
        response.code() shouldBeEqualTo 200
        // response.message().shouldBeNullOrBlank()
    }

    @Test
    fun `parse error response body`() {
        val expectedResponseBody = "ARGHH"
        server.enqueue(MockResponse().setResponseCode(500).setBody(expectedResponseBody))

        val response = api.get().execute()
        response.code() shouldBeEqualTo 500
        response.errorBody()?.byteString()?.toString() shouldBeEqualTo "[text=$expectedResponseBody]"
    }

    @Test
    fun `parse un authorized response body`() {
        val expectedResponseBody = "ARGHH"
        server.enqueue(MockResponse().setResponseCode(401).setBody(expectedResponseBody))

        val response = api.postForString("HELLO").execute()

        response.code() shouldBeEqualTo 401
        response.errorBody()?.byteString()?.toString() shouldBeEqualTo "[text=$expectedResponseBody]"
    }


    /**
     * This shows that is a no-op or otherwise doesn't cause an NPE when there's no content.
     *
     * [204 No Content](https://developer.mozilla.org/ko/docs/Web/HTTP/Status/204)
     */
    @Test
    fun `safe rebuffering`() {
        // NO CONTENT -> CANCEL을 호출하고, 클라가 캐시에서 가져오도록 한다
        server.enqueue(MockResponse().setResponseCode(204))
        api.post("foo").execute()
    }

    @Test
    fun `non response body for POST`() {
        server.enqueue(MockResponse())
        api.noPostBody().execute()
    }

    @Test
    fun `non response body for PUT`() {
        server.enqueue(MockResponse())
        api.noPutBody().execute()
    }

    @Test
    fun `non response body for PATCH`() {
        server.enqueue(MockResponse())
        api.noPatchBody().execute()
    }

    @Test
    fun `parse response missing length`() {
        server.enqueue(MockResponse().setChunkedBody("foo", 1))

        val response = api.post("testing").execute()

        response.code() shouldBeEqualTo 200
        response.message() shouldBeEqualTo "OK"
        response.body() shouldBeEqualTo "foo"
    }


    @Test
    fun `response length not provided`() {
        server.enqueue(MockResponse().setBody("test"))

        val response = api.post("").execute()
        response.body()?.length shouldBeEqualTo 4
    }

    @Test
    fun `contentType with charset`() {
        val expected = "AAAAAAA"
        server.enqueue(MockResponse().setBody(expected))

        val response = api.postWithContentType("foo", "text/plain; charset=utf-8").execute()
        response.body() shouldBeEqualTo expected
    }

    @EnabledOnOs(OS.MAC)
    @Test
    fun `content type defaults to request charset`() {
        server.enqueueBody("foo", "content-type: text/plain; charset=utf-8")

        val expectedBody = "안녕하세요-àáâãäåèéêë"
        api.postWithContentType(expectedBody, "text/plain; charset=utf-8").execute()

        server.takeRequest().body.readUtf8() shouldBeEqualTo expectedBody
    }

    @Test
    fun `default collection format`() {
        server.enqueue(MockResponse().setBody("body"))

        val response = api.get(listOf("bar", "baz")).execute()

        response.code() shouldBeEqualTo 200
        response.message() shouldBeEqualTo "OK"

        server.takeRequest().path shouldBeEqualTo "/?foo=bar&foo=baz"
    }

    @Test
    fun `headers with null params`() {
        server.enqueue(MockResponse().setBody("body"))

        val response = api.getWithHeaders(null).execute()

        response.code() shouldBeEqualTo 200
        response.message() shouldBeEqualTo "OK"

        with(server.takeRequest()) {
            method shouldBeEqualTo "GET"
            path shouldBeEqualTo "/"
            headers["Authorization"].shouldBeNull()
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

        val result = api.get().execute().body()
        result.shouldNotBeNull() shouldBeEqualTo responseData
    }

    @Test
    open fun `can support Deflate`() {
        val responseData = "Compressed Data"
        val compressed = deflate(responseData)
        log.debug { "compresed=${compressed.toUtf8String()}" }
        server.enqueue(

            MockResponse()
                .addHeader("Content-Encoding", "deflate")
                .setBody(Buffer().write(compressed))
        )

        val result = api.get().execute().body()
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

        val result = api.get().execute().body()
        result shouldBeEqualTo responseData
    }


    private fun gzip(data: String): ByteArray {
        return Compressors.GZip.compress(data.toUtf8Bytes())
    }

    private fun deflate(data: String): ByteArray {
        return Compressors.Deflate.compress(data.toUtf8Bytes())
    }
}
