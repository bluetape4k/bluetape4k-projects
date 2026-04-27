package io.bluetape4k.http.hc5.classic

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.junit.jupiter.api.Test

class MinimalAndVirtualThreadHttpClientTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `minimalHttpClientOf 기본 생성`() {
        val client = minimalHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `minimalHttpClientOf 커넥션 매니저 포함 생성`() {
        val cm = httpClientConnectionManager { }
        val client = minimalHttpClientOf(cm)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `minimalHttpClientOf 로 GET 요청`() {
        minimalHttpClientOf().use { client ->
            val response = client.execute(HttpGet("$httpbinBaseUrl/get")) { it }
            log.debug { "GET $httpbinBaseUrl/get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `virtualThreadHttpClientOf 기본 생성`() {
        val client = virtualThreadHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `virtualThreadHttpClientOf 최대 연결 수 설정`() {
        val client = virtualThreadHttpClientOf(maxConnTotal = 50, maxConnPerRoute = 10)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `virtualThreadHttpClientOf 로 GET 요청`() {
        virtualThreadHttpClientOf().use { client ->
            val response = client.execute(HttpGet("$httpbinBaseUrl/get")) { it }
            log.debug { "VirtualThread GET $httpbinBaseUrl/get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }
}
