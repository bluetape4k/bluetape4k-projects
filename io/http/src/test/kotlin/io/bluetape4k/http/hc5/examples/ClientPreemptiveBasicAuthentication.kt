package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.http.hc5.http.httpClientContext
import io.bluetape4k.http.hc5.http.httpHostOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

/**
 * An example of how HttpClient can be customized to authenticate preemptively using Basic scheme.
 *
 * Generally, preemptive authentication can be considered less
 * secure than a response to an authentication challenge and therefore discouraged.
 */
class ClientPreemptiveBasicAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `use preemptive basic authentication`() {

        val httpclient = httpClientOf()

        httpclient.use {
            val context = httpClientContext {
                preemptiveBasicAuth(
                    httpHostOf(httpbinBaseUrl),
                    // HttpHost("http", httpbinServer.host, httpbinServer.port),
                    UsernamePasswordCredentials("user", "passwd".toCharArray())
                )
            }

            val request = HttpGet("$httpbinBaseUrl/hidden-basic-auth/user/passwd")
            log.debug { "Execute request ${request.method} ${request.uri}" }

            repeat(3) {
                val response = httpclient.execute(request, context) { it }

                log.debug { "-------------------" }
                log.debug { "$request  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200
            }
        }
    }
}
