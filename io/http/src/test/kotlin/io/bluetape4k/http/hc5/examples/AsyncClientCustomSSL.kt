package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.httpAsyncClientOf
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.async.suspendExecute
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
import org.junit.jupiter.api.Test
import java.security.cert.X509Certificate

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

            val response = client.suspendExecute(request, context = clientContext)

            log.debug { "$request -> ${StatusLine(response)}" }
            log.debug { response.body }
            response.code shouldBeEqualTo 200
        }
    }
}
