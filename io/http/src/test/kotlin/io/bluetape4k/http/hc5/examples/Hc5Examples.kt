package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.defaultHttpClient
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Hc5Examples: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `1초 후 요청 취소를 수행합니다`() {
        defaultHttpClient().use { httpclient ->
            val httpget = HttpGet("$httpbinBaseUrl/get")
            val executor = Executors.newScheduledThreadPool(1)
            executor.schedule(
                {
                    httpget.cancel()
                    log.debug { "$httpget is canceled by timeout." }
                },
                1,
                TimeUnit.SECONDS
            )
            log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

            try {
                httpclient.execute(httpget) { response ->
                    log.debug { "-------------" }
                    log.debug { "$httpget -> ${StatusLine(response)}" }
                    EntityUtils.consume(response.entity)
                }
            } catch (e: Throwable) {
                // ignore exception
                log.warn(e) { "Fail to request with 1 second timeout" }
            } finally {
                executor.shutdown()
            }
        }
    }

    @Test
    fun `authentication 정보 제공`() {
        val username = "user"
        val password = "passwd"
        val credentialsProvider = CredentialsProviderBuilder.create()
            .add(HttpHost(httpbinServer.host, httpbinServer.port), username, password.toCharArray())
            .build()

        httpClient {
            setDefaultCredentialsProvider(credentialsProvider)
        }
            .use { httpclient ->
                val httpget = HttpGet("$httpbinBaseUrl/basic-auth/$username/$password")
                log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

                httpclient.execute(httpget) { response ->
                    log.debug { "-------------" }
                    log.debug { "$httpget -> ${StatusLine(response)}" }
                    response.entity.consume()
                }
            }
    }
}
