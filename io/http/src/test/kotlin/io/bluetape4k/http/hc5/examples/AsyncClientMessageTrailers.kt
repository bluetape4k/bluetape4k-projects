package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.execute
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.reactor.ioReactorConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.AsyncExecChainHandler
import org.apache.hc.client5.http.impl.ChainElement
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.entity.DigestingEntityProducer
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

class AsyncClientMessageTrailers: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `request interceptor and execution interceptor`() = runTest {
        val target = HttpHost(httpbinServer.host, httpbinServer.port)
        val path = "/post"

        val ioReactorConfig = ioReactorConfig {
            setSoTimeout(Timeout.ofSeconds(5))
        }

        val client: CloseableHttpAsyncClient = httpAsyncClient {
            setIOReactorConfig(ioReactorConfig)

            // 요청 전송 시 AsyncExecChainHandler를 이용하여 MD5 hash 로 변환하도록 한다  
            // Send MD5 hash in a trailer by decorating the original entity producer
            addExecInterceptorAfter(ChainElement.PROTOCOL.name, "custom", asyncExecChainHandler())
        }

        client.start()

        val request = simpleHttpRequestOf(Method.POST, target, path)

        log.debug { "Executing request $request" }
        val response = client.execute(request)
        log.debug { "Response: $request -> ${StatusLine(response)}" }
        log.debug { "Body: ${response.body}" }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }

    // 요청 전송 시 AsyncExecChainHandler를 이용하여 MD5 hash 로 변환하도록 한다  
    // Send MD5 hash in a trailer by decorating the original entity producer


    private fun asyncExecChainHandler(): AsyncExecChainHandler {
        return AsyncExecChainHandler { request, entityProducer, scope, chain, asyncExecCallback ->
            log.debug { "AsyncExecChainHandler request=$request" }
            chain.proceed(
                request,
                entityProducer?.let { DigestingEntityProducer("MD5", it) },
                scope,
                asyncExecCallback
            )
        }
    }
}
