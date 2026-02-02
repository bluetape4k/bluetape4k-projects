package io.bluetape4k.http.hc5.examples

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.async.suspendExecute
import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.http.hc5.reactor.ioReactorConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.AsyncExecChainHandler
import org.apache.hc.client5.http.impl.ChainElement
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequestInterceptor
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.impl.BasicEntityDetails
import org.apache.hc.core5.http.message.BasicHttpResponse
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class AsyncClientInterceptors: AbstractHc5Test() {

    companion object: KLoggingChannel() {
        private val counter = atomic(0L)
    }

    @Test
    fun `request interceptor and execution interceptor`() = runTest {
        val target = HttpHost(httpbinServer.host, httpbinServer.port)
        val path = "/get"

        val ioReactorConfig = ioReactorConfig {
            setSoTimeout(Timeout.ofSeconds(5))
        }

        val client: CloseableHttpAsyncClient = httpAsyncClient {
            setIOReactorConfig(ioReactorConfig)

            // Add a simple request ID to each outgoing request
            addRequestInterceptorFirst(requestInterceptor())

            // Simulate a 404 response for some requests without passing the message down to the backend

            addExecInterceptorAfter(ChainElement.PROTOCOL.name, "custom", asyncExecChainHandler())
        }

        client.start()

        fastList(20) {
            val request = simpleHttpRequestOf(Method.GET, target, path)

            // FIXME: Coroutines 방식으로는 ExecInterceptorAfter 가 먼저 실행되어 버린다???
            val response = client.suspendExecute(request)
            log.debug { "Response: $request -> ${StatusLine(response)}" }
            log.debug { "Body: ${response.body}" }
        }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }

    private fun requestInterceptor(): HttpRequestInterceptor {
        return HttpRequestInterceptor { request, entity, context ->
            request.setHeader("request-id", counter.incrementAndGet().toString())
            log.debug { "request-id = ${request.getFirstHeader("request-id")}" }
        }
    }

    // Simulate a 404 response for some requests without passing the message down to the backend
    // FIXME: 왜 이 놈이 request interceptor 보다 먼저 실행되지 ???

    private fun asyncExecChainHandler(): AsyncExecChainHandler {
        return AsyncExecChainHandler { request, entityProducer, scope, chain, asyncExecCallback ->
            log.debug { "AsyncExecChainHandler request=$request" }
            val idHeader = request.getFirstHeader("request-id")
            log.debug { "idHeader=${idHeader?.value}" }
            if (idHeader?.value == "13") {
                val response = BasicHttpResponse(HttpStatus.SC_NOT_FOUND, "Oppsie")
                val content = ByteBuffer.wrap("bad luck".toUtf8Bytes())
                val asyncDataConsumer = asyncExecCallback.handleResponse(
                    response,
                    BasicEntityDetails(content.remaining().toLong(), ContentTypes.TEXT_PLAIN_UTF8)
                )
                asyncDataConsumer.consume(content)
                asyncDataConsumer.streamEnd(null)
            } else {
                chain.proceed(request, entityProducer, scope, asyncExecCallback)
            }
        }
    }
}
