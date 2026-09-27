package io.bluetape4k.http.hc5.http

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class HttpRequestExtensionsTest {

    companion object: KLogging()

    @Test
    fun `extractPathPrefix - GET 요청에서 prefix 추출`() {
        val request = classicRequest("GET") {
            setUri("https://example.com/api/v1/users")
        }
        request.extractPathPrefix() shouldBeEqualTo "/api/v1/"
    }

    @Test
    fun `extractPathPrefix - 루트 경로 요청에서 prefix 추출`() {
        val request = classicRequest("GET") {
            setUri("https://example.com/")
        }
        request.extractPathPrefix() shouldBeEqualTo "/"
    }

    @Test
    fun `extractPathPrefix - POST 요청에서 prefix 추출`() {
        val request = classicRequest("POST") {
            setUri("https://api.example.com/v2/data/submit")
        }
        request.extractPathPrefix() shouldBeEqualTo "/v2/data/"
    }

    @Test
    fun `extractPathPrefix - 단순 경로 요청에서 prefix 추출`() {
        val request = classicRequest("GET") {
            setUri("http://localhost:8080/health")
        }
        request.extractPathPrefix() shouldBeEqualTo "/"
    }
}
