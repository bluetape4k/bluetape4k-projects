package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import org.apache.hc.client5.http.classic.ExecChain
import org.apache.hc.client5.http.classic.ExecChainHandler
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.ChainElement
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpRequestInterceptor
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicClassicHttpResponse
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

class ClientInterceptors: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `client interceptors`() {
        val httpclient = httpClient {

            // Add a simple request ID to each outgoing request
            addRequestInterceptorFirst(requestInterceptor())

            // FIXME: 왜 이게 먼저 실행되는지? 
            // Simulate a 404 response for some requests without passing the message down to the backend
            addExecInterceptorAfter(ChainElement.PROTOCOL.name, "custom", execChainHandler())
        }

        httpclient.use {
            repeat(20) {
                val httpget = HttpGet("$httpbinBaseUrl/get")
                log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

                val response = httpclient.execute(httpget) { it }
                log.debug { "------------------" }
                log.debug { "$httpget -> ${StatusLine(response)}" }
                response.entity.consume()
            }
        }
    }

    private fun requestInterceptor(): HttpRequestInterceptor {
        val counter = atomic(0L)
        return HttpRequestInterceptor { request: HttpRequest, _, _ ->
            log.debug { "add request-id. ${counter.value}" }
            request.setHeader("request-id", counter.incrementAndGet().toString())
        }
    }

    // Simulate a 404 response for some requests without passing the message down to the backend

    private fun execChainHandler(): ExecChainHandler {
        return ExecChainHandler { request: ClassicHttpRequest, scope: ExecChain.Scope, chain: ExecChain ->
            val idHeader = request.getFirstHeader("request-id")
            log.debug { "idHeader=$idHeader" }

            if (idHeader?.value.equals("13", ignoreCase = true)) {
                BasicClassicHttpResponse(HttpStatus.SC_NOT_FOUND, "Oppsie").apply {
                    entity = StringEntity("bad luck", ContentType.TEXT_PLAIN)
                }
            } else {
                chain.proceed(request, scope)
            }
        }
    }
}
