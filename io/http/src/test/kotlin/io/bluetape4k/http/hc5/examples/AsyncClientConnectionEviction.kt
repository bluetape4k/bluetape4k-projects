package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.executeSuspending
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.reactor.ioReactorConfig
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class AsyncClientConnectionEviction: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `evict expired and idle connection from connection pool`() = runSuspendIO {

        val ioReactorConfig = ioReactorConfig {
            setSoTimeout(Timeout.ofSeconds(3))
        }

        val client = httpAsyncClient {
            setIOReactorConfig(ioReactorConfig)
            evictExpiredConnections()
            evictIdleConnections(TimeValue.ofSeconds(5))
        }

        val httpHost = HttpHost(httpbinServer.host, httpbinServer.port)

        client.use {
            client.start()

            val request = simpleHttpRequest(Method.GET) {
                setHttpHost(httpHost)
                setPath("/")
            }

            log.debug { "Executing request $request" }

            val response = client.executeSuspending(request)

            log.debug { "$request -> ${StatusLine(response)}" }
            log.debug { response.body }

            delay(5.seconds)

            // 이 시점에는 이전 연결이 풀에서 제거되어야 합니다.

            log.debug { "Executing request2 $request" }

            val response2 = client.executeSuspending(request)

            log.debug { "$request -> ${StatusLine(response2)}" }
            log.debug { response2.body }
        }
    }
}
