package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.httpAsyncClientOf
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.ssl.sslContext
import io.bluetape4k.http.hc5.ssl.tlsStrategyOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.security.cert.X509Certificate

/**
 * 외부 TLS 인증서 검증 테스트를 위해 `nghttp2.org`가 필요합니다.
 * 기본값에서는 비활성화되며, `-Dbluetape4k.test.external-network=true`로 활성화할 수 있습니다.
 */
@Tag("external-network")
@EnabledIfSystemProperty(named = "bluetape4k.test.external-network", matches = "true")
class AsyncClientCustomSSL: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `create secure connections with a custom SSL context`() = runTest {
        val sslContext = sslContext {
            loadTrustMaterial { chain, authType ->
                val cert: X509Certificate = chain[0]
                "CN=nghttp2.org".equals(cert.subjectX500Principal.name, ignoreCase = true)
            }
        }
        val tlsStrategy = tlsStrategyOf(sslContext)

        val cm = asyncClientConnectionManager {
            setTlsStrategy(tlsStrategy)
        }

        httpAsyncClientOf(cm).use { client ->
            client.start()

            val target = HttpHost("https", "nghttp2.org")
            val clientContext = HttpClientContext.create()

            val request = simpleHttpRequest(Method.GET) {
                setHttpHost(target)
                setPath("/httpbin")
            }

            log.debug { "Executing request $request" }

            val response = client.executeSuspending(request, context = clientContext)

            log.debug { "$request -> ${StatusLine(response)}" }
            log.debug { response.body }
            response.code shouldBeEqualTo 200
        }
    }
}
