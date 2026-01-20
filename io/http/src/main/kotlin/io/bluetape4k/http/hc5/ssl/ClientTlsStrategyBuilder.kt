package io.bluetape4k.http.hc5.ssl

import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.core5.http.nio.ssl.TlsStrategy
import org.apache.hc.core5.reactor.ssl.SSLBufferMode
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

/**
 * [builder]를 이용해 [ClientTlsStrategyBuilder] 를 빌드합니다.
 *
 * ```
 * val tlsStrategy = tlsStrategy {
 *     setSslContext(sslContext)
 *     setTlsVersions("TLSv1.2")
 *     setCiphers("TLS_RSA_WITH_AES_128_CBC_SHA")
 *     setSslBufferMode(SSLBufferMode.STATIC)
 *     setHostnameVerifier(hostnameVerifier)
 * }
 * ```
 *
 * @param builder [ClientTlsStrategyBuilder] 초기화 람다
 * @return [TlsStrategy]
 */
inline fun tlsStrategy(
    @BuilderInference builder: ClientTlsStrategyBuilder.() -> Unit,
): TlsStrategy =
    ClientTlsStrategyBuilder.create().apply(builder).buildAsync()

/**
 * [sslContext]를 이용해 [TlsStrategy] 를 생성합니다.
 *
 * ```
 * val tlsStrategy = tlsStrategyOf(
 *     sslContext = sslContext,
 *     tlsVersions = arrayOf("TLSv1.2"),
 *     ciphers = arrayOf("TLS_RSA_WITH_AES_128_CBC_SHA"),
 *     sslBufferMode = SSLBufferMode.STATIC,
 *     hostnameVerifier = hostnameVerifier,
 * )
 * ```
 *
 * @param sslContext [SSLContext]
 * @param tlsVersions TLS Versions
 * @param ciphers Ciphers
 * @param sslBufferMode SSL Buffer Mode
 * @param hostnameVerifier Hostname Verifier
 * @param builder [ClientTlsStrategyBuilder] 초기화 람다
 * @return [TlsStrategy]
 */
inline fun tlsStrategyOf(
    sslContext: SSLContext? = null,
    tlsVersions: Array<String>? = null,
    ciphers: Array<String>? = null,
    sslBufferMode: SSLBufferMode = SSLBufferMode.STATIC,
    hostnameVerifier: HostnameVerifier = defaultHostnameVerifier,
    @BuilderInference builder: ClientTlsStrategyBuilder.() -> Unit = {},
): TlsStrategy =
    tlsStrategy {
        sslContext?.run { setSslContext(sslContext) }
        tlsVersions?.run { setTlsVersions(*tlsVersions) }
        ciphers?.run { setCiphers(*ciphers) }
        setSslBufferMode(sslBufferMode)
        setHostnameVerifier(hostnameVerifier)

        builder()
    }
