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
 * Basic 스킴 선인증(preemptive authentication)을 사용하도록
 * HttpClient를 구성하는 예제입니다.
 *
 * 일반적으로 선인증은 인증 챌린지에 대한 응답 방식보다 보안상 불리할 수 있습니다.
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
