package io.bluetape4k.http.hc5.async.methods

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.BasicHeader
import org.junit.jupiter.api.Test

class SimpleHttpRequestTest {

    companion object: KLogging()

    @Test
    fun `simpleHttpRequest method string DSL 로 요청 생성`() {
        val request = simpleHttpRequest("GET") {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1")
        }
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `simpleHttpRequest Method enum DSL 로 요청 생성`() {
        val request = simpleHttpRequest(Method.POST) {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1")
        }
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
    }

    @Test
    fun `simpleHttpRequestOf method string host path 로 요청 생성`() {
        val host = HttpHost("localhost", 8080)
        val request = simpleHttpRequestOf("GET", host, "/api/v1")
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
        request.path shouldBeEqualTo "/api/v1"
    }

    @Test
    fun `simpleHttpRequestOf Method enum host path 로 요청 생성`() {
        val host = HttpHost("localhost", 8080)
        val request = simpleHttpRequestOf(Method.GET, host, "/api/v1")
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `simpleHttpRequestOf body builder 포함 요청 생성`() {
        val host = HttpHost("localhost", 8080)
        val request = simpleHttpRequestOf("POST", host, "/api/v1") {
            setBody("Hello", ContentType.TEXT_PLAIN)
        }
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
    }

    @Test
    fun `simpleHttpRequestOf headers 포함 요청 생성`() {
        val host = HttpHost("localhost", 8080)
        val headers = listOf(BasicHeader("X-Custom", "value"))
        val request = simpleHttpRequestOf("GET", host, "/api/v1", headers = headers)
        request.shouldNotBeNull()
    }

    @Test
    fun `toProducer 로 SimpleRequestProducer 생성`() {
        val request = simpleHttpRequest("GET") {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1")
        }
        val producer = request.toProducer()
        producer.shouldNotBeNull()
    }
}
