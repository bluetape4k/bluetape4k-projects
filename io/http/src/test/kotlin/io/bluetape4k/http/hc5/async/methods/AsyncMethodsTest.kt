package io.bluetape4k.http.hc5.async.methods

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.net.URIAuthority
import org.junit.jupiter.api.Test

class AsyncMethodsTest {

    companion object: KLogging()

    @Test
    fun `simpleRequestProducerOf 로 SimpleRequestProducer 생성`() {
        val request = SimpleHttpRequest.create("GET", "https://example.com/api")
        val producer = simpleRequestProducerOf(request)
        producer.shouldNotBeNull()
    }

    @Test
    fun `simpleResponseConsumerOf 로 SimpleResponseConsumer 생성`() {
        val consumer = simpleResponseConsumerOf()
        consumer.shouldNotBeNull()
    }

    @Test
    fun `simpleResponseConsumerOf 는 SimpleResponseConsumer create 와 동일`() {
        val consumer1 = simpleResponseConsumerOf()
        val consumer2 = SimpleResponseConsumer.create()
        consumer1.shouldNotBeNull()
        consumer2.shouldNotBeNull()
    }

    @Test
    fun `configurableHttpRequestOf host path 로 요청 생성`() {
        val host = HttpHost("localhost", 8080)
        val request = configurableHttpRequestOf("GET", host, "/api/v1")
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }

    @Test
    fun `configurableHttpRequestOf scheme authority path 로 요청 생성`() {
        val authority = URIAuthority("localhost", 8080)
        val request = configurableHttpRequestOf("POST", "/api/v1", scheme = "http", authority = authority)
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "POST"
    }

    @Test
    fun `configurableHttpRequestOf path only 로 요청 생성`() {
        val request = configurableHttpRequestOf("GET", "/api/v1")
        request.shouldNotBeNull()
        request.method shouldBeEqualTo "GET"
    }
}
