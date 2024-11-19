package io.bluetape4k.http.hc5.examples

import io.bluetape4k.coroutines.support.coAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.http.basicHttpRequestOf
import io.bluetape4k.http.hc5.http.toProducer
import io.bluetape4k.http.hc5.reactor.ioReactorConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import java.nio.CharBuffer

class AsyncClientHttpExchangeStreaming: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `asynchronous HTTP 1_1 request with response streaming`() = runTest {
        val target = HttpHost(httpbinServer.host, httpbinServer.port)
        val requestUris = listOf("/", "/ip", "/user-agent", "/headers")

        val ioReactorConfig = ioReactorConfig {
            setSoTimeout(Timeout.ofSeconds(5))
        }

        val client: CloseableHttpAsyncClient = httpAsyncClient {
            setIOReactorConfig(ioReactorConfig)
        }

        // NOTE: 먼저 start() 를 호출해주어야 합니다.
        client.start()

        requestUris.map { path ->
            val request = basicHttpRequestOf(Method.GET, target, path)

            client.execute(
                request.toProducer(),
                charStreamResponseConsumer(request),
                null
            ).coAwait()
        }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }

    private fun charStreamResponseConsumer(request: HttpRequest): AbstractCharResponseConsumer<Void?> {
        return object: AbstractCharResponseConsumer<Void?>() {

            override fun start(response: HttpResponse?, contentType: ContentType?) {
                log.debug { "$request -> ${StatusLine(response)}" }
            }

            override fun capacityIncrement(): Int = Int.MAX_VALUE

            override fun data(src: CharBuffer, endOfStream: Boolean) {
                // Response 를 Streaming 으로 받습니다.
                while (src.hasRemaining()) {
                    print("${src.get()}")
                }
                if (endOfStream) {
                    println()
                }
            }

            override fun buildResult(): Void? = null

            override fun failed(cause: Exception?) {
                log.warn(cause) { "Fail" }
            }

            override fun releaseResources() {}
        }
    }
}
