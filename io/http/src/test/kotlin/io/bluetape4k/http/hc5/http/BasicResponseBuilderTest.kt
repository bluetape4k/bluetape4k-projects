package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.http.message.BasicHttpResponse
import org.junit.jupiter.api.Test

class BasicResponseBuilderTest {

    companion object : KLogging()

    @Test
    fun `basicHttpResponse(Int) - 200 상태코드 검증`() {
        val response = basicHttpResponse(200) {}

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
    }

    @Test
    fun `basicHttpResponse(Int) - 404 상태코드 검증`() {
        val response = basicHttpResponse(404) {}

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 404
    }

    @Test
    fun `basicHttpResponse(Int) - 500 상태코드 검증`() {
        val response = basicHttpResponse(500) {}

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 500
    }

    @Test
    fun `basicHttpResponse(Int) - 헤더 추가 검증`() {
        val response = basicHttpResponse(200) {
            addHeader("Content-Type", "application/json")
            addHeader("X-Request-Id", "abc-123")
        }

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
        response.getFirstHeader("Content-Type")?.value shouldBeEqualTo "application/json"
        response.getFirstHeader("X-Request-Id")?.value shouldBeEqualTo "abc-123"
    }

    @Test
    fun `basicHttpResponse(HttpResponse) - 기존 응답 복사 후 검증`() {
        val original = BasicHttpResponse(201)
        original.addHeader("X-Original", "yes")

        val response = basicHttpResponse(original) {
            addHeader("X-Extra", "extra-value")
        }

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 201
        response.getFirstHeader("X-Original")?.value shouldBeEqualTo "yes"
        response.getFirstHeader("X-Extra")?.value shouldBeEqualTo "extra-value"
    }

    @Test
    fun `basicHttpResponse(Int) - 201 상태코드 검증`() {
        val response = basicHttpResponse(201) {}

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 201
    }

    @Test
    fun `basicHttpResponse(Int) - 302 상태코드 검증`() {
        val response = basicHttpResponse(302) {
            addHeader("Location", "https://example.com/new-location")
        }

        response.shouldNotBeNull()
        response.code shouldBeEqualTo 302
        response.getFirstHeader("Location")?.value shouldBeEqualTo "https://example.com/new-location"
    }
}
