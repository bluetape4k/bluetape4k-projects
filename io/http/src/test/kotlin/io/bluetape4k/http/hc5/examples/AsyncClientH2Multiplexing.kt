package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Test
import java.net.URI

class AsyncClientH2Multiplexing: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `concurrent (multiplexed) execution of multiple HTTP 2 message exchanges`() = runTest {
        val baseUri = URI(NGHTTP2_HTTPBIN_URL)
        val httpHost = HttpHost(baseUri.scheme, baseUri.host, baseUri.port)

        val client = minimalHttpAsyncClientOf()
        client.start()

        val requestUris = listOf("/ip", "/user-agent", "/headers")
        requestUris.forEach { requestUri ->
            val request = simpleHttpRequest(Method.GET) {
                setHttpHost(httpHost)
                setPath(requestUri)
            }

            log.debug { "Executing request $request" }

            val response = client.executeSuspending(request)

            log.debug { "$request -> ${StatusLine(response)}" }
            log.debug { response.body }
        }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
