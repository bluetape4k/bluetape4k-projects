package io.bluetape4k.http.hc5.examples

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
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

    companion object: KLogging() {
        private const val EXECUTION_ID_HEADER = "execution-id"
        private const val REQUEST_ID_HEADER = "request-id"
    }

    @Test
    fun `client interceptors`() {
        val events = mutableListOf<String>()

        val httpclient = httpClient {

            // Request protocol interceptors run inside MainClientExec, after custom exec interceptors.
            addRequestInterceptorFirst(requestInterceptor(events))

            // Some requests are handled without reaching the backend.
            addExecInterceptorAfter(ChainElement.PROTOCOL.name, "custom", execChainHandler(events))
        }

        val statuses = mutableListOf<Int>()

        httpclient.use {
            repeat(20) {
                val executionId = (it + 1).toString()
                val httpget = HttpGet("$httpbinBaseUrl/get")
                httpget.setHeader(EXECUTION_ID_HEADER, executionId)
                log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

                val status = httpclient.execute(httpget) { response ->
                    log.debug { "------------------" }
                    log.debug { "$httpget -> ${StatusLine(response)}" }
                    response.entity.consume()
                    response.code
                }
                statuses.add(status)
            }
        }

        statuses shouldBeEqualTo expectedStatuses()
        events shouldBeEqualTo expectedEvents()
    }

    private fun requestInterceptor(events: MutableList<String>): HttpRequestInterceptor {

        return HttpRequestInterceptor { request: HttpRequest, _, _ ->
            val executionId = request.getFirstHeader(EXECUTION_ID_HEADER)?.value ?: "missing"
            events.add("request:$executionId")
            request.setHeader(REQUEST_ID_HEADER, "request-$executionId")
            log.debug { "request-id = ${request.getFirstHeader(REQUEST_ID_HEADER)}" }
        }
    }

    private fun execChainHandler(events: MutableList<String>): ExecChainHandler {
        return ExecChainHandler { request: ClassicHttpRequest, scope: ExecChain.Scope, chain: ExecChain ->
            val executionId = request.getFirstHeader(EXECUTION_ID_HEADER)?.value ?: "missing"
            events.add("exec-before:$executionId:${request.getFirstHeader(REQUEST_ID_HEADER)?.value ?: "missing"}")
            log.debug { "executionId=$executionId" }

            if (executionId == "13") {
                events.add("exec-short-circuit:$executionId")
                BasicClassicHttpResponse(HttpStatus.SC_NOT_FOUND, "Oppsie").apply {
                    entity = StringEntity("bad luck", ContentType.TEXT_PLAIN)
                }
            } else {
                chain.proceed(request, scope).also {
                    events.add("exec-after:$executionId:${request.getFirstHeader(REQUEST_ID_HEADER)?.value ?: "missing"}")
                }
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
