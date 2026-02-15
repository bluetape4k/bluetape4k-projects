package io.bluetape4k.http.hc5.examples

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http.executeSuspending
import io.bluetape4k.http.hc5.http.tlsConfigOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.AsyncClientEndpoint
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/**
 * 외부 HTTP/2 테스트 엔드포인트(`nghttp2.org`)가 필요합니다.
 * 기본값에서는 비활성화되며, `-Dbluetape4k.test.external-network=true`로 활성화할 수 있습니다.
 */
@Tag("external-network")
@EnabledIfSystemProperty(named = "bluetape4k.test.external-network", matches = "true")
class AsyncClientH2Multiplexing: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `concurrent (multiplexed) execution of multiple HTTP 2 message exchanges`() = runTest {

        // HTTP/2 검증은 https://nghttp2.org/httpbin/post 등 nghttp2.org 엔드포인트를 사용합니다.
        val httpHost = HttpHost("https", "nghttp2.org")

        val client = minimalHttpAsyncClientOf(
            connMgr = asyncClientConnectionManager {
                setDefaultTlsConfig(tlsConfigOf(versionPolicy = HttpVersionPolicy.FORCE_HTTP_2))
            }
        )
        client.start()

        val endpoint: AsyncClientEndpoint = client.lease(httpHost, null).suspendAwait()
        val requestUris = listOf("/httpbin/ip", "/httpbin/user-agent", "/httpbin/headers")
        // val latch = CountDownLatch(requestUris.size)

        try {
            requestUris.forEach { requestUri ->
                val request = simpleHttpRequest(Method.GET) {
                    setHttpHost(httpHost)
                    setPath(requestUri)
                }

                log.debug { "Executing request $request" }

                val response = endpoint.executeSuspending(request)

                log.debug { "$request -> ${StatusLine(response)}" }
                log.debug { response.body }
            }

        } finally {
            endpoint.releaseAndReuse()
        }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
