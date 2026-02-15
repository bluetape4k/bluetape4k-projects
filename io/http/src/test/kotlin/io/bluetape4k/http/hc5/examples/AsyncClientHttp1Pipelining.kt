package io.bluetape4k.http.hc5.examples

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http.executeSuspending
import io.bluetape4k.http.hc5.http.tlsConfig
import io.bluetape4k.http.hc5.http.toProducer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.RequestNotExecutedException
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Test

class AsyncClientHttp1Pipelining: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `pipelined execution of multiple HTTP 1_1 message exchanges`() = runTest {
        val target = HttpHost(httpbinServer.host, httpbinServer.port)
        val requestUris = listOf("/get", "/ip", "/user-agent", "/headers")

        val cm = asyncClientConnectionManager {
            setDefaultTlsConfig(tlsConfig { setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1) })
        }
        val client = minimalHttpAsyncClientOf(connMgr = cm)
        client.start()

        var endpoint = client.lease(target, null).awaitSuspending()

        requestUris.forEach { path ->
            val request = simpleHttpRequestOf(Method.GET, target, path)
            log.debug { "Executing request $request" }

            try {
                endpoint.executeSuspending(
                    request.toProducer(),
                    SimpleResponseConsumer.create()
                ).apply {
                    log.debug { "Response: $request -> ${StatusLine(this)}" }
                    log.debug { "Body: ${this.body}" }
                }
            } catch (_: RequestNotExecutedException) {
                // httpbin-http2(h2c 프록시) 환경에서 HTTP/1.1 연결이 닫힐 수 있어 endpoint를 재획득합니다.
                endpoint = client.lease(target, null).awaitSuspending()
                endpoint.executeSuspending(
                    request.toProducer(),
                    SimpleResponseConsumer.create()
                ).apply {
                    log.debug { "Response: $request -> ${StatusLine(this)}" }
                    log.debug { "Body: ${this.body}" }
                }
            }
        }

        endpoint.releaseAndReuse()

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
