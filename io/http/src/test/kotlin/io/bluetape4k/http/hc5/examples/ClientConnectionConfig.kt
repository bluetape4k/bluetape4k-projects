package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.http.hc5.http.classicRequest
import io.bluetape4k.http.hc5.http.connectionConfig
import io.bluetape4k.http.hc5.http.defaultTlsConfig
import io.bluetape4k.http.hc5.http.poolingHttpClientConnectionManager
import io.bluetape4k.http.hc5.http.tlsConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.URIScheme
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

/** 라우트 또는 호스트 단위로 연결 설정을 다르게 적용하는 예제입니다. */
class ClientConnectionConfig: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `use connection configuration on a per-route or a per-host`() {

        val cm = poolingHttpClientConnectionManager {
            setConnectionConfigResolver { route ->
                // 보안(TLS) 연결에는 다른 설정을 적용합니다.
                if (route.isSecure) {
                    connectionConfig {
                        setConnectTimeout(Timeout.ofMinutes(2))
                        setSocketTimeout(Timeout.ofMinutes(2))
                        setValidateAfterInactivity(TimeValue.ofMinutes(1))
                        setTimeToLive(TimeValue.ofHours(1))
                    }
                } else {
                    connectionConfig {
                        setConnectTimeout(Timeout.ofMinutes(1))
                        setSocketTimeout(Timeout.ofMinutes(1))
                        setValidateAfterInactivity(TimeValue.ofSeconds(15))
                        setTimeToLive(TimeValue.ofMinutes(15))
                    }
                }
            }

            setTlsConfigResolver { host ->
                // 특정 호스트에는 별도 TLS 설정을 적용합니다.
                if (host.schemeName.equals("https", true)) {
                    tlsConfig {
                        setSupportedProtocols(*TLS.entries.toTypedArray())
                        setHandshakeTimeout(Timeout.ofSeconds(10))
                    }
                } else {
                    defaultTlsConfig
                }
            }
        }

        // CredentialsProvider를 설정합니다.
        val httpclient = httpClient { setConnectionManager(cm) }

        httpclient.use {
            listOf(URIScheme.HTTP).forEach { uriScheme ->
                val request = classicRequest(Method.GET) {
                    setHttpHost(HttpHost(uriScheme.id, httpbinServer.host, httpbinServer.port))
                    setPath("/headers")
                }
                log.debug { "Execute request ${request.method} ${request.uri}" }

                val response = httpclient.execute(request) { it }

                log.debug { "-------------------" }
                log.debug { "$request  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200
            }
        }
    }
}
