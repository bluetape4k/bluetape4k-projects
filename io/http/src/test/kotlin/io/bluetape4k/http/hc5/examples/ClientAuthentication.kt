package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.auth.credentialsProviderOf
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

/** 사용자 인증이 필요한 대상 서버에 요청하는 HttpClient 예제입니다. */
class ClientAuthentication: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `request against target site that requires user authentication`() {

        val httpHost = HttpHost(httpbinServer.host, httpbinServer.port)

        // CredentialsProvider를 설정합니다.
        val httpclient = httpClient {
            setDefaultCredentialsProvider(
                credentialsProviderOf(httpHost, "user", "passwd".toCharArray())
            )
        }

        httpclient.use {
            val httpget = HttpGet("${httpHost.toURI()}/basic-auth/user/passwd")
            log.debug { "Execute request ${httpget.method} ${httpget.uri}" }

            httpclient.execute(httpget) { response ->
                log.debug { "-------------------" }
                log.debug { "$httpget  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200
            }
        }
    }
}
