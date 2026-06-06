package io.bluetape4k.http.hc5.examples

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.http.hc5.reactor.ioReactorConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.AsyncExecCallback
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
import java.util.concurrent.CopyOnWriteArrayList

class AsyncClientInterceptors: AbstractHc5Test() {

    companion object: KLoggingChannel() {
        private const val EXECUTION_ID_HEADER = "execution-id"
        private const val REQUEST_ID_HEADER = "request-id"
    }

    @Test
    fun `request interceptor and execution interceptor`() = runTest {
        val target = HttpHost("http", httpbinServer.host, httpbinServer.port)
        val path = "/httpbin/get"
        val events = CopyOnWriteArrayList<String>()

        val ioReactorConfig = ioReactorConfig {
            setSoTimeout(Timeout.ofSeconds(5))
        }

        val client: CloseableHttpAsyncClient = httpAsyncClient {
            setIOReactorConfig(ioReactorConfig)

            // Request protocol interceptors run inside HttpAsyncMainClientExec, after custom exec interceptors.
            addRequestInterceptorFirst(requestInterceptor(events))

            // Some requests are handled without reaching the backend.
            addExecInterceptorAfter(ChainElement.PROTOCOL.name, "custom", asyncExecChainHandler(events))
        }

        client.start()

        try {
            val statuses = List(20) {
                val executionId = (it + 1).toString()
                val request = simpleHttpRequestOf(Method.GET, target, path)
                request.setHeader(EXECUTION_ID_HEADER, executionId)

                val response = client.executeSuspending(request)
                log.debug { "Response: $request -> ${StatusLine(response)}" }
                log.debug { "Body: ${response.body}" }
                response.code
            }

            statuses shouldBeEqualTo expectedStatuses()
            events.toList() shouldBeEqualTo expectedEvents()
        } finally {
            log.debug { "Shutting down" }
            client.close(CloseMode.GRACEFUL)
        }
    }

    private fun requestInterceptor(events: MutableList<String>): HttpRequestInterceptor {
        return HttpRequestInterceptor { request, _, _ ->
            val executionId = request.getFirstHeader(EXECUTION_ID_HEADER)?.value ?: "missing"
            events.add("request:$executionId")
            request.setHeader(REQUEST_ID_HEADER, "request-$executionId")
            log.debug { "request-id = ${request.getFirstHeader(REQUEST_ID_HEADER)}" }
        }
    }

    private fun asyncExecChainHandler(
        events: MutableList<String>,
    ): AsyncExecChainHandler {
        return AsyncExecChainHandler { request, entityProducer, scope, chain, asyncExecCallback ->
            log.debug { "AsyncExecChainHandler request=$request" }
            val executionId = request.getFirstHeader(EXECUTION_ID_HEADER)?.value ?: "missing"
            events.add("exec-before:$executionId:${request.getFirstHeader(REQUEST_ID_HEADER)?.value ?: "missing"}")
            log.debug { "executionId=$executionId" }
            if (executionId == "13") {
                events.add("exec-short-circuit:$executionId")
                val response = BasicHttpResponse(HttpStatus.SC_NOT_FOUND, "Oppsie")
                val content = ByteBuffer.wrap("bad luck".toUtf8Bytes())
                val asyncDataConsumer = asyncExecCallback.handleResponse(
                    response,
                    BasicEntityDetails(content.remaining().toLong(), ContentTypes.TEXT_PLAIN_UTF8)
                )
                asyncDataConsumer.consume(content)
                asyncDataConsumer.streamEnd(null)
                asyncExecCallback.completed()
            } else {
                chain.proceed(
                    request,
                    entityProducer,
                    scope,
                    object: AsyncExecCallback by asyncExecCallback {
                        override fun completed() {
                            events.add(
                                "exec-after:$executionId:${request.getFirstHeader(REQUEST_ID_HEADER)?.value ?: "missing"}"
                            )
                            asyncExecCallback.completed()
                        }
                    }
                )
            }
        }
    }

    private fun expectedStatuses(): List<Int> =
        (1..20).map { executionId ->
            if (executionId == 13) HttpStatus.SC_NOT_FOUND else HttpStatus.SC_OK
        }

    private fun expectedEvents(): List<String> =
        (1..20).flatMap { executionId ->
            if (executionId == 13) {
                listOf(
                    "exec-before:$executionId:missing",
                    "exec-short-circuit:$executionId",
                )
            } else {
                listOf(
                    "exec-before:$executionId:missing",
                    "request:$executionId",
                    "exec-after:$executionId:request-$executionId",
                )
            }
        }
}
