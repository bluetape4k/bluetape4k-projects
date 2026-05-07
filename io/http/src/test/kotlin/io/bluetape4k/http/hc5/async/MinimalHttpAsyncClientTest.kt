package io.bluetape4k.http.hc5.async

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.impl.async.MinimalH2AsyncClient
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient
import org.apache.hc.core5.http2.config.H2Config
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class MinimalHttpAsyncClientTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `minimalHttpAsyncClientOf 기본 생성`() {
        val client: MinimalHttpAsyncClient = minimalHttpAsyncClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `minimalH2AsyncClientOf H2Config 로 생성`() {
        val h2config = H2Config.DEFAULT
        val client: MinimalH2AsyncClient = minimalH2AsyncClientOf(h2config)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `minimalHttpAsyncClientOf asyncClientConnectionManager 포함 생성`() {
        val cm = asyncClientConnectionManager { }
        val client: MinimalHttpAsyncClient = minimalHttpAsyncClientOf(connMgr = cm)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `defaultMinimalHttpAsyncClient 싱글톤 생성`() {
        defaultMinimalHttpAsyncClient.shouldNotBeNull()
    }

    @Test
    fun `defaultMinimalH2AsyncClient 싱글톤 생성`() {
        defaultMinimalH2AsyncClient.shouldNotBeNull()
    }

    @Test
    fun `leaseSuspending 으로 엔드포인트 획득`() = runTest(timeout = 15.seconds) {
        val cm = asyncClientConnectionManager { }
        val client = minimalHttpAsyncClientOf(connMgr = cm)
        client.start()
        try {
            val url = java.net.URL(httpbinBaseUrl)
            val host = org.apache.hc.core5.http.HttpHost(url.protocol, url.host, url.port)
            val endpoint = client.leaseSuspending(host)
            log.debug { "Leased endpoint: $endpoint" }
            endpoint.shouldNotBeNull()
            endpoint.releaseAndDiscard()
        } finally {
            client.close()
        }
    }
}
