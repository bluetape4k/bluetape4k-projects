package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.async.suspendExecute
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

class AsyncClientHttpExchange: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `asynchronous HTTP 1_1 request in coroutines`() = runTest {
        val target = HttpHost(httpbinServer.host, httpbinServer.port)
        val requestUris = listOf("/", "/ip", "/user-agent", "/headers")

        val ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(Timeout.ofSeconds(5))
            .build()

        val client: CloseableHttpAsyncClient = HttpAsyncClients.custom()
            .setIOReactorConfig(ioReactorConfig)
            .build()

        // NOTE: 먼저 start() 를 호출해주어야 합니다.
        client.start()

        val responses = requestUris.map { path ->
            val request = simpleHttpRequestOf(Method.GET, target, path)
            async(Dispatchers.IO) {
                client.suspendExecute(request)
                    .apply {
                        log.debug { "Response: $request -> ${StatusLine(this)}" }
                        log.debug { "Body: ${this.body}" }
                    }
            }
        }

        responses.awaitAll()

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
