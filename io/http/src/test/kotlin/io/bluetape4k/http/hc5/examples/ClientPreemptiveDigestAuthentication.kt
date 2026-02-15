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

/** 로컬 httpbin 서버를 사용한 선인증(Preemptive Authentication) 예제입니다. */
class ClientPreemptiveDigestAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    private val localHttpbinBaseUrl get() = httpbinBaseUrl
    private val username = "debop"
    private val password = "bluetape4k"

    @Test
    fun `use preemptive basic authentication`() {

        val httpclient = httpClientOf()
        val httpHost = httpHostOf(localHttpbinBaseUrl)

        httpclient.use {
            val localContext = httpClientContext {
                useCredentialsProvider(
                    credentialsProvider {
                        add(httpHost, UsernamePasswordCredentials(username, password.toCharArray()))
                    }
                )
            }
            val request = HttpGet("$localHttpbinBaseUrl/basic-auth/$username/$password")
            log.debug { "Execute request ${request.method} ${request.uri}" }

            repeat(1) {
                val response = httpclient.execute(request, localContext) { it }

                log.debug { "-------------------" }
                log.debug { "$request  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200

                val authExchange = localContext.getAuthExchange(httpHost)
                if (authExchange != null) {
                    when (val authScheme = authExchange.authScheme) {
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
        val httpHost = httpHostOf(localHttpbinBaseUrl)

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
                .workers(2)
                .rounds(2)
                .add {
                    val localContext = localContextStorage.get()

                    val request = HttpGet("$localHttpbinBaseUrl/basic-auth/$username/$password")
                    log.debug { "Execute request ${request.method} ${request.uri}" }

                    httpclient.execute(request, localContext) { response ->
                        log.debug { "-------------------" }
                        log.debug { "$request  -> ${StatusLine(response)}" }
                        // response.entity?.consume()
                        response.code shouldBeEqualTo 200

                        val authExchange = localContext.getAuthExchange(httpHost)!!
                        when (val authScheme = authExchange.authScheme) {
                            is BasicScheme -> log.debug { "Basic auth scheme: ${authScheme.name}, ${authScheme.realm}" }
                            is DigestScheme -> log.debug { "Digest auth scheme: ${authScheme.name}, count: ${authScheme.nounceCount}" }
                        }
                    }
                }
                .run()
        }
    }
}
