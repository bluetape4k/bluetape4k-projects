package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.BasicHttpRequest
import org.apache.hc.core5.http.nio.support.BasicRequestProducer
import org.junit.jupiter.api.Test
import java.net.URI

class BasicRequestProducerTest {

    companion object: KLogging()

    private val testUri: URI = URI.create("http://localhost:8080/api/v1")

    @Test
    fun `BasicHttpRequest toProducer 로 BasicRequestProducer 생성`() {
        val request = BasicHttpRequest("GET", testUri)
        val producer = request.toProducer()
        producer.shouldNotBeNull()
    }

    @Test
    fun `basicRequestProducerOf request 로 생성`() {
        val request = BasicHttpRequest("GET", testUri)
        val producer = basicRequestProducerOf(request)
        producer.shouldNotBeNull()
    }

    @Test
    fun `basicRequestProducerOf Method URI 로 생성`() {
        val producer: BasicRequestProducer = basicRequestProducerOf(Method.GET, testUri)
        producer.shouldNotBeNull()
    }

    @Test
    fun `basicRequestProducerOf methodName URI 로 생성`() {
        val producer: BasicRequestProducer = basicRequestProducerOf("POST", testUri)
        producer.shouldNotBeNull()
    }

    @Test
    fun `basicRequestProducerOf dataProducer null 로 생성`() {
        val request = BasicHttpRequest(Method.GET.name, testUri)
        val producer = basicRequestProducerOf(request, dataProducer = null)
        producer.shouldNotBeNull()
    }
}
