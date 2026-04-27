package io.bluetape4k.http.hc5.routing

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.message.BasicHttpRequest
import org.junit.jupiter.api.Test

class RoutingSupportTest {

    companion object: KLogging()

    @Test
    fun `determineHost - URI 포함 요청에서 호스트 결정`() {
        val request = BasicHttpRequest("GET", "https://example.com/api/v1")
        val host = request.determineHost()
        host.shouldNotBeNull()
        host.hostName shouldBeEqualTo "example.com"
        host.schemeName shouldBeEqualTo "https"
    }

    @Test
    fun `determineHost - HTTP URI 포함 요청에서 호스트 결정`() {
        val request = BasicHttpRequest("GET", "http://api.example.com/data")
        val host = request.determineHost()
        host.shouldNotBeNull()
        host.hostName shouldBeEqualTo "api.example.com"
        host.schemeName shouldBeEqualTo "http"
    }

    @Test
    fun `normalize - http 기본 포트 정규화`() {
        val host = HttpHost("http", "example.com", 80)
        val normalized = host.normalize()
        normalized.shouldNotBeNull()
        normalized.schemeName shouldBeEqualTo "http"
    }

    @Test
    fun `normalize - https 기본 포트 정규화`() {
        val host = HttpHost("https", "example.com", 443)
        val normalized = host.normalize()
        normalized.shouldNotBeNull()
        normalized.schemeName shouldBeEqualTo "https"
    }

    @Test
    fun `normalize - 커스텀 SchemePortResolver 사용`() {
        val host = HttpHost("https", "example.com", 8443)
        val normalized = host.normalize(DefaultSchemePortResolver.INSTANCE)
        normalized.shouldNotBeNull()
        normalized.hostName shouldBeEqualTo "example.com"
    }
}
