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
import org.apache.hc.client5.http.impl.auth.DigestScheme
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

/**
 * An example of how HttpClient can authenticate multiple requests
 * using the same Digest scheme. After the initial request / response exchange
 * all subsequent requests sharing the same execution context can re-use
 * the last Digest nonce value to authenticate with the server.
 */
class ClientPreemptiveDigestAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `use preemptive basic authentication`() {

        val httpclient = httpClientOf()
        val httpHost = httpHostOf(httpbinBaseUrl)

        httpclient.use {
            val localContext = httpClientContext {
                useCredentialsProvider(
                    credentialsProvider {
                        add(httpHost, UsernamePasswordCredentials("user", "passwd".toCharArray()))
                    }
                )
            }
            val request = HttpGet("$httpbinBaseUrl/digest-auth/auth/user/passwd")
            log.debug { "Execute request ${request.method} ${request.uri}" }

            repeat(3) {
                val response = httpclient.execute(request, localContext) { it }

                log.debug { "-------------------" }
                log.debug { "$request  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200

                val authExchange = localContext.getAuthExchange(httpHost)
                if (authExchange != null) {
                    val authScheme = authExchange.authScheme
                    if (authScheme is DigestScheme) {
                        log.debug { "Nonce: ${authScheme.nonce}; count: ${authScheme.nounceCount}" }
                    }
                }
            }
        }
    }

    @Test
    fun `use preemptive basic authentication in multi threading`() {

        val httpclient = httpClientOf()
        val httpHost = httpHostOf(httpbinBaseUrl)

        val localContextStorage = ThreadLocal.withInitial {
            httpClientContext {
                useCredentialsProvider(
                    credentialsProvider {
                        add(httpHost, UsernamePasswordCredentials("user", "passwd".toCharArray()))
                    }
                )
            }
        }

        httpclient.use {
            MultithreadingTester()
                .numThreads(4)
                .roundsPerThread(4)
                .add {
                    val localContext = localContextStorage.get()

                    val request = HttpGet("$httpbinBaseUrl/digest-auth/auth/user/passwd")
                    log.debug { "Execute request ${request.method} ${request.uri}" }

                    httpclient.execute(request, localContext) { response ->
                        log.debug { "-------------------" }
                        log.debug { "$request  -> ${StatusLine(response)}" }
                        // response.entity?.consume()
                        response.code shouldBeEqualTo 200

                        val authExchange = localContext.getAuthExchange(httpHost)!!
                        val authScheme = authExchange.authScheme as DigestScheme
                        log.debug { "Nonce: ${authScheme.nonce}; count: ${authScheme.nounceCount}" }
                    }
                }
                .run()
        }
    }
}
