package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.auth.credentialsProvider
import io.bluetape4k.http.hc5.classic.httpClientOf
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.http.hc5.http.httpClientContext
import io.bluetape4k.http.hc5.http.httpHostOf
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.auth.BasicScheme
import org.apache.hc.client5.http.impl.auth.DigestScheme
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

/**
 * nghttp2.org/httpbin 에서 제공하는 HTTP Basic Authentication 예제입니다.
 */
class ClientPreemptiveDigestAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    private val httpbinHost = "https://nghttp2.org"
    private val httpbinBaseUrl = "$httpbinHost/httpbin"
    private val username = "debop"
    private val password = "bluetape4k"

    @Test
    fun `use preemptive basic authentication`() {

        val httpclient = httpClientOf()
        val httpHost = httpHostOf(httpbinHost)

        httpclient.use {
            val localContext = httpClientContext {
                useCredentialsProvider(
                    credentialsProvider {
                        add(httpHost, UsernamePasswordCredentials(username, password.toCharArray()))
                    }
                )
            }
            val request = HttpGet("$httpbinBaseUrl/basic-auth/$username/$password")
            log.debug { "Execute request ${request.method} ${request.uri}" }

            repeat(1) {
                val response = httpclient.execute(request, localContext) { it }

                log.debug { "-------------------" }
                log.debug { "$request  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200

                val authExchange = localContext.getAuthExchange(httpHost)
                if (authExchange != null) {
                    val authScheme = authExchange.authScheme
                    when (authScheme) {
                        is BasicScheme -> log.debug { "Basic auth scheme: ${authScheme.name}, ${authScheme.realm}" }
                        is DigestScheme -> log.debug { "Digest auth scheme: ${authScheme.name}, count: ${authScheme.nounceCount}" }
                    }
                }
            }
        }
    }

    @Test
    fun `use preemptive basic authentication in multi threading`() {

        val httpclient = httpClientOf()
        val httpHost = httpHostOf(httpbinHost)

        val localContextStorage = ThreadLocal.withInitial {
            httpClientContext {
                useCredentialsProvider(
                    credentialsProvider {
                        add(httpHost, UsernamePasswordCredentials(username, password.toCharArray()))
                    }
                )
            }
        }

        httpclient.use {
            MultithreadingTester()
                .numThreads(2)
                .roundsPerThread(2)
                .add {
                    val localContext = localContextStorage.get()

                    val request = HttpGet("$httpbinBaseUrl/basic-auth/$username/$password")
                    log.debug { "Execute request ${request.method} ${request.uri}" }

                    httpclient.execute(request, localContext) { response ->
                        log.debug { "-------------------" }
                        log.debug { "$request  -> ${StatusLine(response)}" }
                        // response.entity?.consume()
                        response.code shouldBeEqualTo 200

                        val authExchange = localContext.getAuthExchange(httpHost)!!
                        val authScheme = authExchange.authScheme
                        when (authScheme) {
                            is BasicScheme -> log.debug { "Basic auth scheme: ${authScheme.name}, ${authScheme.realm}" }
                            is DigestScheme -> log.debug { "Digest auth scheme: ${authScheme.name}, count: ${authScheme.nounceCount}" }
                        }
                    }
                }
                .run()
        }
    }
}
