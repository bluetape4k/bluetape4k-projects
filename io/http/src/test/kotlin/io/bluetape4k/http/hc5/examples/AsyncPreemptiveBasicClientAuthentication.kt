package io.bluetape4k.http.hc5.examples

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequestOf
import io.bluetape4k.http.hc5.async.suspendExecute
import io.bluetape4k.http.hc5.http.httpClientContext
import io.bluetape4k.http.hc5.http.toProducer
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncPreemptiveBasicClientAuthentication: AbstractHc5Test() {

    companion object: KLoggingChannel()

    private lateinit var client: CloseableHttpAsyncClient

    @BeforeEach
    fun beforeEach() {
        client = httpAsyncClient {}
        client.start()
    }

    @AfterEach
    fun afterEach() {
        client.close(CloseMode.GRACEFUL)
    }

    @Test
    fun `request interceptor and execution interceptor`() = runSuspendIO {
        val httpHost = HttpHost("http", httpbinServer.host, httpbinServer.port)
        val path = "/basic-auth/user/passwd"

        val localContext = httpClientContext {
            preemptiveBasicAuth(httpHost, UsernamePasswordCredentials("user", "passwd".toCharArray()))
        }

        repeat(3) {
            val request = simpleHttpRequestOf(Method.GET, httpHost, path)
            log.debug { "Executing request $request" }
            val response = client.suspendExecute(request, localContext)

            log.debug { "Response: $request -> ${StatusLine(response)}" }
            log.debug { "Body: ${response.body}" }
        }

        val deferreds = List(10) {
            val request = simpleHttpRequestOf(Method.GET, httpHost, path)
            log.debug { "Executing request concurrently $request" }

            async(Dispatchers.IO) {
                client
                    .execute(
                        request.toProducer(),
                        SimpleResponseConsumer.create(),
                        localContext,
                        null
                    )
                    .suspendAwait()
                    .also { response ->
                        log.debug { "Response: $request -> ${StatusLine(response)}" }
                        log.debug { "Body: ${response.body.bodyText}" }

                    }
            }
        }
        deferreds.awaitAll()

        log.debug { "Shutting down" }
    }
}
