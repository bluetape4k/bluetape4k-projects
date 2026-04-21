package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * mkcert로 생성된 로컬 CA 인증서를 신뢰하는 SSL 컨텍스트 헬퍼.
 *
 * `testing/testcontainers/src/main/resources/certs/rootCA.pem`에 번들된 CA 인증서를 사용하여
 * [BluetapeHttpServer] 및 [BluetapeWebfluxServer]의 HTTPS 엔드포인트를 검증하는
 * [X509TrustManager], [SSLContext], OkHttp 클라이언트 빌더 설정을 제공한다.
 *
 * 시스템 trust store를 수정하지 않으며, CI 환경에서도 별도 설치 없이 동작한다.
 *
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .let { BluetapeSslContext.configureOkHttp(it) }
 *     .build()
 * val response = client.newCall(Request.Builder().url(server.httpsUrl + "/ping").build()).execute()
 * ```
 */
object BluetapeSslContext: KLogging() {

    private const val CA_CERT_RESOURCE = "/certs/rootCA.pem"

    /**
     * 번들된 CA 인증서만 신뢰하는 [X509TrustManager]를 생성한다.
     */
    fun createTrustManager(): X509TrustManager {
        val caStream = BluetapeSslContext::class.java.getResourceAsStream(CA_CERT_RESOURCE)
            ?: error("$CA_CERT_RESOURCE 를 classpath에서 찾을 수 없습니다")

        val cf = CertificateFactory.getInstance("X.509")
        val caCert = cf.generateCertificate(caStream) as X509Certificate

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also {
            it.load(null, null)
            it.setCertificateEntry("bluetape4k-ca", caCert)
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        return tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /**
     * 번들된 CA 인증서만 신뢰하는 [SSLContext]를 생성한다.
     */
    fun createSslContext(): SSLContext {
        val trustManager = createTrustManager()
        return SSLContext.getInstance("TLS").also {
            it.init(null, arrayOf(trustManager), null)
        }
    }

    /**
     * OkHttp 빌더에 Bluetape4k CA 인증서 신뢰 설정을 적용한다.
     *
     * `localhost` 및 `127.0.0.1`에 대한 hostname 검증을 허용한다.
     *
     * ```kotlin
     * val client = BluetapeSslContext.configureOkHttp(OkHttpClient.Builder()).build()
     * ```
     *
     * @param builder 설정할 [OkHttpClient.Builder]
     * @return SSL이 설정된 [OkHttpClient.Builder]
     */
    fun configureOkHttp(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        val trustManager = createTrustManager()
        val sslContext = SSLContext.getInstance("TLS").also {
            it.init(null, arrayOf(trustManager), null)
        }
        return builder
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { hostname, _ ->
                hostname == "localhost" || hostname == "127.0.0.1"
            }
    }
}
