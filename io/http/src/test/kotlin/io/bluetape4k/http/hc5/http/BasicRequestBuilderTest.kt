package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.BasicHeader
import org.junit.jupiter.api.Test

class BasicRequestBuilderTest {

    companion object : KLogging()

    @Test
    fun `basicHttpRequest(String) - GET 메서드 이름 검증`() {
        val request = basicHttpRequest("GET") {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `basicHttpRequest(String) - POST 메서드 이름 검증`() {
        val request = basicHttpRequest("POST") {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1/users")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
    }

    @Test
    fun `basicHttpRequest(Method) - Method 열거형 사용 검증`() {
        val request = basicHttpRequest(Method.DELETE) {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1/users/1")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "DELETE"
    }

    @Test
    fun `basicHttpRequest(Method) - PUT 메서드 검증`() {
        val request = basicHttpRequest(Method.PUT) {
            setHttpHost(HttpHost("localhost", 8080))
            setPath("/api/v1/items/42")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "PUT"
    }

    @Test
    fun `basicHttpRequestOf - host와 path 검증`() {
        val host = HttpHost("localhost", 8080)
        val request = basicHttpRequestOf("GET", host, "/api/v1")

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
        request.path shouldBeEqualTo "/api/v1"
        request.authority?.hostName shouldBeEqualTo "localhost"
        request.authority?.port shouldBeEqualTo 8080
    }

    @Test
    fun `basicHttpRequestOf(Method) - Method 열거형으로 host와 path 검증`() {
        val host = HttpHost("example.com", 9090)
        val request = basicHttpRequestOf(Method.GET, host, "/health")

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
        request.path shouldBeEqualTo "/health"
        request.authority?.hostName shouldBeEqualTo "example.com"
        request.authority?.port shouldBeEqualTo 9090
    }

    @Test
    fun `basicHttpRequestOf - 헤더 포함 요청 검증`() {
        val host = HttpHost("localhost", 8080)
        val headers = listOf(
            BasicHeader("Authorization", "Bearer token"),
            BasicHeader("Accept", "application/json"),
        )
        val request = basicHttpRequestOf("GET", host, "/api/v1", headers)

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
        request.getFirstHeader("Authorization")?.value shouldBeEqualTo "Bearer token"
        request.getFirstHeader("Accept")?.value shouldBeEqualTo "application/json"
    }

    @Test
    fun `basicHttpRequestOf - DSL builder 추가 헤더 검증`() {
        val host = HttpHost("localhost", 8080)
        val request = basicHttpRequestOf("POST", host, "/api/v1/data") {
            addHeader("Content-Type", "application/json")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
        request.getFirstHeader("Content-Type")?.value shouldBeEqualTo "application/json"
    }
}
