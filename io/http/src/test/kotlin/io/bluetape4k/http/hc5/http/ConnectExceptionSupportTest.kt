package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.HttpHostConnectException
import org.apache.hc.core5.net.URIAuthority
import org.junit.jupiter.api.Test
import java.io.IOException

class ConnectExceptionSupportTest {

    companion object : KLogging()

    @Test
    fun `IOException toConnectTimeoutException - ConnectTimeoutException 타입 반환 검증`() {
        val cause = IOException("Connection timed out")
        val endpoint = URIAuthority("example.com", 443)

        val result = cause.toConnectTimeoutException(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf ConnectTimeoutException::class
    }

    @Test
    fun `IOException toConnectTimeoutException - 포트 없는 엔드포인트 검증`() {
        val cause = IOException("Timeout")
        val endpoint = URIAuthority("api.example.com", 80)

        val result = cause.toConnectTimeoutException(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf ConnectTimeoutException::class
    }

    @Test
    fun `IOException toHttpHostConnectException - HttpHostConnectException 타입 반환 검증`() {
        val cause = IOException("Connection refused")
        val endpoint = URIAuthority("example.com", 443)

        val result = cause.toHttpHostConnectException(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf HttpHostConnectException::class
    }

    @Test
    fun `IOException toHttpHostConnectException - 다른 포트 엔드포인트 검증`() {
        val cause = IOException("Host unreachable")
        val endpoint = URIAuthority("internal.service.com", 8080)

        val result = cause.toHttpHostConnectException(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf HttpHostConnectException::class
    }

    @Test
    fun `IOException enhance - IOException 반환 확인`() {
        val cause = IOException("Network error")
        val endpoint = URIAuthority("example.com", 443)

        val result = cause.enhance(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf IOException::class
    }

    @Test
    fun `IOException enhance - 엔드포인트 정보 보강 후 IOException 반환`() {
        val cause = IOException("Read timed out")
        val endpoint = URIAuthority("slow.server.com", 9000)

        val result = cause.enhance(endpoint)

        result.shouldNotBeNull()
        result shouldBeInstanceOf IOException::class
    }
}
