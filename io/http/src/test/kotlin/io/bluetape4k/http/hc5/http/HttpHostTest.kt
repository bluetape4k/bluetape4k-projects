package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.net.URI

class HttpHostTest {

    companion object : KLogging()

    @Test
    fun `URI toHttpHost - HTTP URL 파싱`() {
        val uri = URI("http://example.com")
        val httpHost = uri.toHttpHost()

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "http"
        httpHost.hostName shouldBeEqualTo "example.com"
    }

    @Test
    fun `URI toHttpHost - HTTPS URL 파싱`() {
        val uri = URI("https://example.com")
        val httpHost = uri.toHttpHost()

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "https"
        httpHost.hostName shouldBeEqualTo "example.com"
    }

    @Test
    fun `URI toHttpHost - 포트 포함 URL 파싱`() {
        val uri = URI("http://localhost:8080")
        val httpHost = uri.toHttpHost()

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "http"
        httpHost.hostName shouldBeEqualTo "localhost"
        httpHost.port shouldBeEqualTo 8080
    }

    @Test
    fun `URI toHttpHost - HTTPS 포트 포함 URL 파싱`() {
        val uri = URI("https://api.example.com:443")
        val httpHost = uri.toHttpHost()

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "https"
        httpHost.hostName shouldBeEqualTo "api.example.com"
        httpHost.port shouldBeEqualTo 443
    }

    @Test
    fun `httpHostOf - HTTP URL 문자열 파싱`() {
        val httpHost = httpHostOf("http://example.com")

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "http"
        httpHost.hostName shouldBeEqualTo "example.com"
    }

    @Test
    fun `httpHostOf - 포트 포함 URL 문자열 파싱`() {
        val httpHost = httpHostOf("http://localhost:9090")

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "http"
        httpHost.hostName shouldBeEqualTo "localhost"
        httpHost.port shouldBeEqualTo 9090
    }

    @Test
    fun `httpHostOf - HTTPS URL 문자열 파싱`() {
        val httpHost = httpHostOf("https://secure.example.com:8443")

        httpHost.shouldNotBeNull()
        httpHost.schemeName shouldBeEqualTo "https"
        httpHost.hostName shouldBeEqualTo "secure.example.com"
        httpHost.port shouldBeEqualTo 8443
    }
}
