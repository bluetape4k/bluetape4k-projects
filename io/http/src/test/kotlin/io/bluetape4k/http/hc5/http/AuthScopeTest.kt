package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http.HttpHost
import org.junit.jupiter.api.Test

class AuthScopeTest {

    companion object : KLogging()

    @Test
    fun `authScopeOf(protocol, host, port) 기본 검증`() {
        val scope = authScopeOf("http", "localhost", 8080)

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "localhost"
        scope.port shouldBeEqualTo 8080
    }

    @Test
    fun `authScopeOf(protocol, host, port, realm, schemeName) 전체 파라미터 검증`() {
        val scope = authScopeOf("http", "example.com", 80, "admin", "BASIC")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "example.com"
        scope.port shouldBeEqualTo 80
        scope.realm shouldBeEqualTo "admin"
        scope.schemeName shouldBeEqualTo "BASIC"
    }

    @Test
    fun `authScopeOf(protocol, host) - 포트 기본값(-1) 검증`() {
        val scope = authScopeOf("https", "secure.example.com")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "secure.example.com"
        scope.port shouldBeEqualTo -1
    }

    @Test
    fun `authScopeOf(protocol, host) - realm null 검증`() {
        val scope = authScopeOf("http", "localhost", 8080)

        scope.shouldNotBeNull()
        scope.realm.shouldBeNull()
    }

    @Test
    fun `authScopeOf(HttpHost) - HttpHost 기반 생성 검증`() {
        val host = HttpHost("http", "localhost", 8080)
        val scope = authScopeOf(host)

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "localhost"
        scope.port shouldBeEqualTo 8080
    }

    @Test
    fun `authScopeOf(HttpHost, realm) - realm 포함 검증`() {
        val host = HttpHost("http", "api.example.com", 443)
        val scope = authScopeOf(host, realm = "admin-area")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "api.example.com"
        scope.port shouldBeEqualTo 443
        scope.realm shouldBeEqualTo "admin-area"
    }

    @Test
    fun `authScopeOf(HttpHost, realm, schemeName) - schemeName 포함 검증`() {
        val host = HttpHost("https", "example.com", 443)
        val scope = authScopeOf(host, realm = "test-realm", schemeName = "DIGEST")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "example.com"
        scope.realm shouldBeEqualTo "test-realm"
        scope.schemeName shouldBeEqualTo "DIGEST"
    }

    @Test
    fun `authScopeOf(url) - URL 문자열 기반 생성 검증`() {
        val scope = authScopeOf("http://localhost:8080")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "localhost"
        scope.port shouldBeEqualTo 8080
    }

    @Test
    fun `authScopeOf(url, realm) - URL과 realm 검증`() {
        val scope = authScopeOf("http://example.com:9000", realm = "secure-zone")

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "example.com"
        scope.port shouldBeEqualTo 9000
        scope.realm shouldBeEqualTo "secure-zone"
    }

    @Test
    fun `authScopeOf(host, port) - 호스트와 포트만으로 생성 검증`() {
        val scope = authScopeOf("api.example.com", 8443)

        scope.shouldNotBeNull()
        scope.host shouldBeEqualTo "api.example.com"
        scope.port shouldBeEqualTo 8443
    }
}
