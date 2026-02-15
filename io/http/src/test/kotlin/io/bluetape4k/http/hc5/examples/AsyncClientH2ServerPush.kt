package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http2.h2Config
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.test.runTest
import org.apache.hc.client5.http.async.methods.AbstractBinPushConsumer
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.ByteBuffer

class AsyncClientH2ServerPush: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `handling HTTP 2 message exchanges pushed by the server`() = runTest {
        val baseUri = URI(NGHTTP2_HTTPBIN_URL)
        val httpHost = HttpHost(baseUri.scheme, baseUri.host, baseUri.port)

        val client = minimalHttpAsyncClientOf(
            h2config = h2Config { setPushEnabled(true) }
        )
        client.start()

        @Suppress("DEPRECATION")
        client.register("*") {
            object: AbstractBinPushConsumer() {
                override fun capacityIncrement(): Int = Int.MAX_VALUE

                override fun releaseResources() {}

                override fun data(src: ByteBuffer?, endOfStream: Boolean) {}

                override fun completed() {}

                override fun start(promise: HttpRequest, response: HttpResponse, contentType: ContentType) {
                    log.debug { "PushConsumer: ${promise.path} (push) -> ${StatusLine(response)}" }
                }

                override fun failed(cause: Exception?) {
                    log.warn(cause) { "(push) -> Failed" }
                }
            }
        }

        val request = simpleHttpRequest(Method.GET) {
            setHttpHost(httpHost)
            setPath("/get")
        }

        log.debug { "Executing request $request" }

        val response = client.executeSuspending(request)

        log.debug { "Response: $request -> ${StatusLine(response)}" }
        log.debug { "Body: ${response.body}" }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
