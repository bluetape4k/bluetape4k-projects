package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.http.Method
import org.junit.jupiter.api.Test
import java.net.URI

class RequestTest {

    companion object: KLogging()

    private val testUri: URI = URI.create("https://example.com/api")
    private val testUriStr: String = "https://example.com/api"

    @Test
    fun `requestOf Method URI 로 GET 요청 생성`() {
        val request = requestOf(Method.GET, testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestOf methodName URI 로 POST 요청 생성`() {
        val request = requestOf("POST", testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestOf methodName String 로 요청 생성`() {
        val request = requestOf("GET", testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestGet URI 로 GET 요청 생성`() {
        val request = requestGet(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestGet String 로 GET 요청 생성`() {
        val request = requestGet(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestHead URI 로 HEAD 요청 생성`() {
        val request = requestHead(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestHead String 로 HEAD 요청 생성`() {
        val request = requestHead(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPost URI 로 POST 요청 생성`() {
        val request = requestPost(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPost String 로 POST 요청 생성`() {
        val request = requestPost(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPatch URI 로 PATCH 요청 생성`() {
        val request = requestPatch(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPatch String 로 PATCH 요청 생성`() {
        val request = requestPatch(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPut URI 로 PUT 요청 생성`() {
        val request = requestPut(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestPut String 로 PUT 요청 생성`() {
        val request = requestPut(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestTrace URI 로 TRACE 요청 생성`() {
        val request = requestTrace(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestTrace String 로 TRACE 요청 생성`() {
        val request = requestTrace(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestDelete URI 로 DELETE 요청 생성`() {
        val request = requestDelete(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestDelete String 로 DELETE 요청 생성`() {
        val request = requestDelete(testUriStr)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestOptions URI 로 OPTIONS 요청 생성`() {
        val request = requestOptions(testUri)
        request.shouldNotBeNull()
    }

    @Test
    fun `requestOptions String 로 OPTIONS 요청 생성`() {
        val request = requestOptions(testUriStr)
        request.shouldNotBeNull()
    }
}
