package io.bluetape4k.http.hc5.async

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class AsyncHttpClientTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `httpAsyncClientOf 기본 생성`() {
        val client = httpAsyncClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `httpAsyncClient DSL 생성`() {
        val client = httpAsyncClient { }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `httpAsyncClientOf 로 비동기 GET 요청 상태코드 200`() {
        httpAsyncClientOf().use { client ->
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val future = client.execute(request, null)
            val response = future.get(10, TimeUnit.SECONDS)
            log.debug { "GET $httpbinBaseUrl/get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `h2AsyncClientOf 생성`() {
        val client = h2AsyncClientOf()
        client.shouldNotBeNull()
        client.close()
    }
}
