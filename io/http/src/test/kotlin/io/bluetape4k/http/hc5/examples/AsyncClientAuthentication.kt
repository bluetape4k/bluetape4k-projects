package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.execute
import io.bluetape4k.http.hc5.async.httpAsyncClient
import io.bluetape4k.http.hc5.async.methods.simpleHttpRequest
import io.bluetape4k.http.hc5.auth.credentialsProviderOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Test

class AsyncClientAuthentication: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `async client authentication`() = runTest {
        val httpHost = HttpHost(httpbinServer.host, httpbinServer.port)
        val httpclient = httpAsyncClient {
            setDefaultCredentialsProvider(
                credentialsProviderOf(
                    httpHost,
                    "user",
                    "passwd".toCharArray()
                )
            )
        }

        httpclient.start()

        val request = simpleHttpRequest(Method.GET) {
            setHttpHost(httpHost)
            setPath("/basic-auth/user/passwd")
        }

        log.debug { "Executing request $request" }

        val response = httpclient.execute(request)

        log.debug { "$request -> ${StatusLine(response)}" }
        log.debug { response.body }

        log.debug { "Shutting down" }
        httpclient.close(CloseMode.GRACEFUL)
    }
}
