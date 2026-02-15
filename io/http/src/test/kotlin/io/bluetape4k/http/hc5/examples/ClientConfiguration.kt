package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.auth.emptyCredentialsProvider
import io.bluetape4k.http.hc5.classic.httpClient
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.http.hc5.http.charCodingConfig
import io.bluetape4k.http.hc5.http.connectionConfig
import io.bluetape4k.http.hc5.http.http1Config
import io.bluetape4k.http.hc5.http.managedHttpConnectionFactory
import io.bluetape4k.http.hc5.http.requestConfig
import io.bluetape4k.http.hc5.http.socketConfig
import io.bluetape4k.http.hc5.http.tlsConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.ContextBuilder
import org.apache.hc.client5.http.HttpRoute
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import org.apache.hc.client5.http.auth.StandardAuthScheme
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.ParseException
import org.apache.hc.core5.http.impl.io.DefaultClassicHttpResponseFactory
import org.apache.hc.core5.http.impl.io.DefaultHttpRequestWriterFactory
import org.apache.hc.core5.http.impl.io.DefaultHttpResponseParserFactory
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.http.message.BasicLineParser
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.util.CharArrayBuffer
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.nio.charset.CodingErrorAction

class ClientConfiguration: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `using client configuration`() {

        // HTTP/1.1 프로토콜 설정을 생성합니다.
        val h1Config = http1Config {
            setMaxHeaderCount(200)
            setMaxLineLength(2000)
        }

        val lineParser = object: BasicLineParser() {
            override fun parseHeader(buffer: CharArrayBuffer?): Header {
                return try {
                    super.parseHeader(buffer)
                } catch (e: ParseException) {
                    BasicHeader(buffer.toString(), null)
                }
            }
        }
        val responseParserFactory = object: DefaultHttpResponseParserFactory(
            h1Config,
            lineParser,
            DefaultClassicHttpResponseFactory.INSTANCE
        ) {}

        val requestWriterFactory = DefaultHttpRequestWriterFactory()

        val charCodingConfig = charCodingConfig {
            setMalformedInputAction(CodingErrorAction.IGNORE)
            setUnmappableInputAction(CodingErrorAction.IGNORE)
            setCharset(Charsets.UTF_8)
        }

        // 사용자 정의 connection factory로 아웃바운드 HTTP 연결 초기화 과정을 커스터마이즈합니다.
        // 표준 연결 설정 외에도 메시지 parser/writer 루틴을 연결별로 지정할 수 있습니다.
        val connFactory = managedHttpConnectionFactory {
            http1Config(h1Config)
            charCodingConfig(charCodingConfig)
            requestWriterFactory(requestWriterFactory)
            responseParserFactory(responseParserFactory)
        }

        // 초기화가 끝난 클라이언트 HTTP 연결 객체는 임의의 네트워크 소켓에 바인딩될 수 있습니다.
        // 소켓 초기화/원격 연결/로컬 바인딩 과정은 connection socket factory가 제어합니다.

        // 보안 연결용 SSL context는 시스템 또는 애플리케이션 설정 기반으로 생성할 수 있습니다.
//        val sslContext = sslContextOfSystem()

        // 지원 프로토콜 스킴별 사용자 정의 connection socket factory registry 예시입니다.
//        val socketFactoryRegistry = registry {
//            register("http", PlainConnectionSocketFactory.INSTANCE)
//            register("https", SSLConnectionSocketFactory(sslContext))
//        }
//        val socketFactoryRegistry = registryOf(
//            mapOf(
//                "http" to PlainConnectionSocketFactory.INSTANCE,
//                "https" to SSLConnectionSocketFactory(sslContext)
//            )
//        )

        // 시스템 DNS 해석을 대체할 사용자 정의 DNS resolver를 사용합니다.
        val dnsResolver = object: SystemDefaultDnsResolver() {
            override fun resolve(host: String): Array<InetAddress> {
                return if (host.equals("myhost", ignoreCase = true)) {
                    arrayOf(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
                } else {
                    super.resolve(host)
                }
            }
        }

        // 사용자 정의 설정으로 connection manager를 생성합니다.
        val connManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setConnectionFactory(connFactory)
            .setDnsResolver(dnsResolver)
            .setDefaultConnectionConfig(connectionConfig {
                setConnectTimeout(Timeout.ofSeconds(30))
                setSocketTimeout(Timeout.ofSeconds(30))
                setTimeToLive(TimeValue.ofMinutes(5))
            })
            .setSocketConfigResolver {
                socketConfig {
                    setTcpNoDelay(true)
                    setSoKeepAlive(true)
                }
            }
            .build()
//        val connManager = PoolingHttpClientConnectionManager(
//            socketFactoryRegistry,
//            PoolConcurrencyPolicy.STRICT,
//            PoolReusePolicy.LIFO,
//            TimeValue.ofMinutes(5),
//            null,
//            dnsResolver,
//            connFactory
//        )

        // connection manager에 기본 또는 특정 호스트별 소켓 설정을 적용할 수 있습니다.
        // connManager.defaultSocketConfig = socketConfig { setTcpNoDelay(true) }

        // 10초 유휴 후 연결 유효성 검사 예시
//        connManager.setDefaultConnectionConfig(
//            connectionConfig {
//                setConnectTimeout(Timeout.ofSeconds(30))
//                setSocketTimeout(Timeout.ofSeconds(30))
//                setValidateAfterInactivity(TimeValue.ofSeconds(10))
//                setTimeToLive(TimeValue.ofHours(1))
//            }
//        )

        // TLS 버전 설정
        connManager.setDefaultTlsConfig(
            tlsConfig {
                setHandshakeTimeout(Timeout.ofSeconds(30))
                setSupportedProtocols(TLS.V_1_0, TLS.V_1_1, TLS.V_1_2, TLS.V_1_3)
            }
        )

        // 풀에 유지되거나 lease될 지속 연결의 전체/라우트별 최대값을 설정합니다.
        connManager.maxTotal = 100
        connManager.defaultMaxPerRoute = 10
        connManager.setMaxPerRoute(HttpRoute(HttpHost("somehost", 80)), 20)

        // 필요 시 사용자 정의 cookie store를 사용합니다.
        val cookieStore = BasicCookieStore()
        // 필요 시 사용자 정의 credentials provider를 사용합니다.
        val credentialsProvider = emptyCredentialsProvider()  //CredentialsProviderBuilder.create().build()
        // 전역 요청 설정을 생성합니다.
        val defaultRequestConfig = requestConfig {
            setCookieSpec(StandardCookieSpec.STRICT)
            setExpectContinueEnabled(true)
            setTargetPreferredAuthSchemes(listOf(StandardAuthScheme.BEARER, StandardAuthScheme.DIGEST))
            setProxyPreferredAuthSchemes(listOf(StandardAuthScheme.BASIC))
        }

        // 위에서 구성한 의존성과 설정으로 HttpClient를 생성합니다.

        val httpclient = httpClient { // HttpClients.custom()
            setConnectionManager(connManager)
            setDefaultCookieStore(cookieStore)
            setDefaultCredentialsProvider(credentialsProvider)
            // setProxy(HttpHost("myproxy", 8080))
            setDefaultRequestConfig(defaultRequestConfig)
        }

        httpclient.use {
            val httpget = HttpGet("$httpbinBaseUrl/get")

            // 요청 단위 설정은 클라이언트 단위 설정보다 우선합니다.
            val requestConfig = RequestConfig.copy(defaultRequestConfig)
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .build()
            httpget.config = requestConfig

            // 실행 컨텍스트는 로컬에서 커스터마이즈할 수 있으며,
            // 로컬 컨텍스트 속성이 클라이언트 컨텍스트보다 우선합니다.
            val context = ContextBuilder.create()
                .useCookieStore(cookieStore)
                .useCredentialsProvider(credentialsProvider)
                .build()

            log.debug { "Executing request ${httpget.method} ${httpget.uri}" }

            val response = httpclient.execute(httpget, context) { it }
            response.entity.consume()

            // 마지막 실행 요청
            log.debug { "request = ${context.request}" }

            // 실행 라우트
            log.debug { "http route = ${context.httpRoute}" }

            // 인증 교환 정보
            log.debug { "auth exchanges = ${context.authExchanges}" }

            // 쿠키 원본
            log.debug { "cookie origin = ${context.cookieOrigin}" }

            // 사용된 쿠키 스펙
            log.debug { "cookie spec = ${context.cookieSpec}" }

            // 사용자 보안 토큰
            log.debug { "user token = ${context.userToken}" }

            log.debug { "context=$context" }
        }
    }
}
