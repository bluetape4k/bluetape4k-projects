package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.auth.credentialsProvider
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.http.hc5.http.authScopeOf
import io.bluetape4k.http.hc5.http.httpHostOf
import io.bluetape4k.http.hc5.http.requestConfigOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * A simple example that uses HttpClient to execute an HTTP request
 * over a secure connection tunneled through an authenticating proxy.
 */
@Disabled("Proxy Server 가 실행되어야 합니다.")
class ClientProxyAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `use authenticating proxy`() {
        val credentialsProvider = credentialsProvider {
            add(authScopeOf("localhost", 8888), "squid", "squid".toCharArray())
            add(authScopeOf(httpbinServer.host, httpbinServer.port), "user", "passwd".toCharArray())
        }
        val target = httpHostOf(httpbinBaseUrl)
        val proxy = HttpHost("localhost", 8888)

        val httpclient = httpClient {
            setProxy(proxy)
            setDefaultCredentialsProvider(credentialsProvider)
        }

        httpclient.use {
            val config = requestConfigOf()
            val httpget = HttpGet("/basic-auth/user/passwd").apply {
                setConfig(config)
            }
            log.debug { "Executing request ${httpget.method} ${httpget.uri} to $target" }

            httpclient.execute(target, httpget) { response ->
                log.debug { "-------------------" }
                log.debug { "$httpget  -> ${StatusLine(response)}" }
                response.entity.consume()
            }
        }
    }
}
