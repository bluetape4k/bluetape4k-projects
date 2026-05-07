package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http.Method
import org.junit.jupiter.api.Test
import java.net.URI

class ClassicRequestBuilderTest {

    companion object : KLogging()

    @Test
    fun `classicRequest(String) - GET 메서드 이름 검증`() {
        val request = classicRequest("GET") {
            setUri("https://example.com/api")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `classicRequest(String) - POST 메서드 이름 검증`() {
        val request = classicRequest("POST") {
            setUri("https://example.com/api/users")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
    }

    @Test
    fun `classicRequest(String) - PUT 메서드 이름 검증`() {
        val request = classicRequest("PUT") {
            setUri("https://example.com/api/users/1")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "PUT"
    }

    @Test
    fun `classicRequest(String) - DELETE 메서드 이름 검증`() {
        val request = classicRequest("DELETE") {
            setUri("https://example.com/api/users/42")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "DELETE"
    }

    @Test
    fun `classicRequest(Method) - Method 열거형 사용 검증`() {
        val request = classicRequest(Method.GET) {
            setUri("https://example.com/health")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `classicRequest(Method) - PATCH 메서드 검증`() {
        val request = classicRequest(Method.PATCH) {
            setUri("https://example.com/api/items/5")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "PATCH"
    }

    @Test
    fun `classicRequest URI 설정 검증`() {
        val uri = URI("https://api.example.com/v2/data")
        val request = classicRequest("GET") {
            setUri(uri)
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
        request.uri shouldBeEqualTo uri
    }

    @Test
    fun `classicRequest 헤더 추가 검증`() {
        val request = classicRequest("POST") {
            setUri("https://example.com/submit")
            addHeader("Authorization", "Bearer my-token")
            addHeader("Content-Type", "application/json")
        }

        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
        request.getFirstHeader("Authorization")?.value shouldBeEqualTo "Bearer my-token"
        request.getFirstHeader("Content-Type")?.value shouldBeEqualTo "application/json"
    }
}
