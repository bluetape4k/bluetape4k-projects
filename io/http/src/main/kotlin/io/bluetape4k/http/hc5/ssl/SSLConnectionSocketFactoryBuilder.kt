package io.bluetape4k.http.hc5.ssl

import io.bluetape4k.support.ifTrue
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

/**
 * [initializer]를 이용해 [SSLConnectionSocketFactoryBuilder] 를 빌드합니다.
 *
 * ```
 * val sslConnectionSocketFactory = sslConnectionSocketFactory {
 *     setSslContext(sslContext)
 *     setTlsVersions("TLSv1.2")
 *     setCiphers("TLS_RSA_WITH_AES_128_CBC_SHA")
 *     setHostnameVerifier(hostnameVerifier)
 *     useSystemProperties()
 * }
 * ```
 *
 * @param initializer [SSLConnectionSocketFactoryBuilder] 초기화 람다
 * @return [SSLConnectionSocketFactory]
 */
inline fun sslConnectionSocketFactory(
    initializer: SSLConnectionSocketFactoryBuilder.() -> Unit,
): SSLConnectionSocketFactory {
    return SSLConnectionSocketFactoryBuilder.create().apply(initializer).build()
}

/**
 * [sslContext]를 이용해 [SSLConnectionSocketFactory] 를 생성합니다.
 *
 * ```
 * val sslConnectionSocketFactory = sslConnectionSocketFactoryOf(
 *     sslContext = sslContext,
 *     tlsVersions = arrayOf("TLSv1.2"),
 *     ciphers = arrayOf("TLS_RSA_WITH_AES_128_CBC_SHA"),
 *     hostnameVerifier = hostnameVerifier,
 *     systemProperties = true,
 * ) {
 *    setSslBufferMode(SSLBufferMode.STATIC)
 * }
 * ```
 *
 * @param sslContext [SSLContext]
 * @param tlsVersions TLS Versions
 * @param ciphers Ciphers
 * @param hostnameVerifier Hostname Verifier
 * @param systemProperties Use System Properties
 * @param initializer [SSLConnectionSocketFactoryBuilder] 초기화 람다
 * @return [SSLConnectionSocketFactory]
 */
inline fun sslConnectionSocketFactoryOf(
    sslContext: SSLContext? = null,
    tlsVersions: Array<String>? = null,
    ciphers: Array<String>? = null,
    hostnameVerifier: HostnameVerifier = defaultHostnameVerifier,
    systemProperties: Boolean? = null,
    initializer: SSLConnectionSocketFactoryBuilder.() -> Unit = {},
): SSLConnectionSocketFactory = sslConnectionSocketFactory {
    sslContext?.run { setSslContext(sslContext) }
    tlsVersions?.run { setTlsVersions(*tlsVersions) }
    ciphers?.run { setCiphers(*ciphers) }
    setHostnameVerifier(hostnameVerifier)
    systemProperties?.ifTrue { useSystemProperties() }

    initializer()
}
