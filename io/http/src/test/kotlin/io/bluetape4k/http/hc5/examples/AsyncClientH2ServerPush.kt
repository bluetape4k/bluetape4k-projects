package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.asyncClientConnectionManager
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http.tlsConfigOf
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
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.nio.ByteBuffer

/**
 * 외부 HTTP/2 서버 푸시 동작 확인을 위해 `nghttp2.org`가 필요합니다.
 * 기본값에서는 비활성화되며, `-Dbluetape4k.test.external-network=true`로 활성화할 수 있습니다.
 */
@Tag("external-network")
@EnabledIfSystemProperty(named = "bluetape4k.test.external-network", matches = "true")
class AsyncClientH2ServerPush: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `handling HTTP 2 message exchanges pushed by the server`() = runTest {

        // HTTP/2 검증은 https://nghttp2.org/httpbin/post 등 nghttp2.org 엔드포인트를 사용합니다.
        val httpHost = HttpHost("https", "nghttp2.org")

        val client = minimalHttpAsyncClientOf(
            h2config = h2Config { setPushEnabled(true) },
            connMgr = asyncClientConnectionManager {
                setDefaultTlsConfig(tlsConfigOf(versionPolicy = HttpVersionPolicy.FORCE_HTTP_2))
            }
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
            setPath("/httpbin/")
        }

        log.debug { "Executing request $request" }

        val response = client.executeSuspending(request)

        log.debug { "Response: $request -> ${StatusLine(response)}" }
        log.debug { "Body: ${response.body}" }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
