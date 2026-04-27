package io.bluetape4k.http.jdk

import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class JdkHttpClientSupportTest: AbstractHttpTest() {

    companion object: KLogging()

    @Test
    fun `jdkHttpClientOf 기본 생성`() {
        val client = jdkHttpClientOf()
        client.shouldNotBeNull()
    }

    @Test
    fun `jdkVirtualThreadHttpClientOf 생성`() {
        val client = jdkVirtualThreadHttpClientOf()
        client.shouldNotBeNull()
    }

    @Test
    fun `JdkHttpClients default 싱글톤 생성`() {
        val client = JdkHttpClients.default
        client.shouldNotBeNull()
    }

    @Test
    fun `JdkHttpClients virtualThread 싱글톤 생성`() {
        val client = JdkHttpClients.virtualThread
        client.shouldNotBeNull()
    }

    @Test
    fun `jdkHttpClientOf 로 GET 요청 상태코드 200`() {
        val client = jdkHttpClientOf()
        val request = HttpRequest.newBuilder(URI.create("$httpbinBaseUrl/get")).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        log.debug { "GET $httpbinBaseUrl/get status=${response.statusCode()}" }
        response.statusCode() shouldBeEqualTo 200
    }
}
