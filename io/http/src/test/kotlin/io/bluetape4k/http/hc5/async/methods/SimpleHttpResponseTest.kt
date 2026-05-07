package io.bluetape4k.http.hc5.async.methods

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http.ContentType
import org.junit.jupiter.api.Test

class SimpleHttpResponseTest {

    companion object: KLogging()

    @Test
    fun `simpleHttpResponse DSL 로 응답 생성`() {
        val response = simpleHttpResponse(200) {
            setBody("Hello, World!", ContentType.TEXT_PLAIN)
        }
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
    }

    @Test
    fun `simpleHttpResponseOf 문자열 본문으로 응답 생성`() {
        val response = simpleHttpResponseOf(200, "Hello, World!")
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
        response.bodyText shouldBeEqualTo "Hello, World!"
    }

    @Test
    fun `simpleHttpResponseOf 문자열 본문과 ContentType 으로 응답 생성`() {
        val response = simpleHttpResponseOf(201, "Created", ContentType.TEXT_PLAIN)
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 201
        response.bodyText shouldBeEqualTo "Created"
    }

    @Test
    fun `simpleHttpResponseOf ByteArray 본문으로 응답 생성`() {
        val body = "Hello".toByteArray()
        val response = simpleHttpResponseOf(200, body)
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
    }

    @Test
    fun `simpleHttpResponseOf ByteArray 본문과 ContentType 으로 응답 생성`() {
        val body = "Data".toByteArray()
        val response = simpleHttpResponseOf(200, body, ContentType.APPLICATION_OCTET_STREAM)
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 200
    }

    @Test
    fun `simpleHttpResponse 404 응답 생성`() {
        val response = simpleHttpResponse(404) {
            setBody("Not Found", ContentType.TEXT_PLAIN)
        }
        response.shouldNotBeNull()
        response.code shouldBeEqualTo 404
    }
}
