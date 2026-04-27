package io.bluetape4k.http.hc5.classic

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.junit.jupiter.api.Test

class ClassicHttpClientTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `httpClient DSL 로 CloseableHttpClient 생성`() {
        val client = httpClient { }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `httpClientOf 기본 생성`() {
        val client = httpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `defaultHttpClient 생성`() {
        val client = defaultHttpClient()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `systemHttpClientOf 생성`() {
        val client = systemHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `httpClientOf 로 GET 요청 상태코드 200`() {
        httpClientOf().use { client ->
            val request = HttpGet("$httpbinBaseUrl/get")
            val statusCode = client.execute(request) { response -> response.code }
            log.debug { "GET $httpbinBaseUrl/get status=$statusCode" }
            statusCode shouldBeEqualTo 200
        }
    }

    @Test
    fun `httpClientConnectionManager DSL 생성 검증`() {
        val cm = httpClientConnectionManager {
            setMaxConnPerRoute(5)
            setMaxConnTotal(10)
        }
        cm.shouldNotBeNull()
    }

    @Test
    fun `httpClientOf 커넥션 매니저 포함 생성`() {
        val cm = httpClientConnectionManager {
            setMaxConnPerRoute(5)
            setMaxConnTotal(10)
        }
        val client = httpClientOf(cm)
        client.shouldNotBeNull()
        client.close()
    }
}
