package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.config.TlsConfig
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.util.Timeout

/**
 * 기본 [TlsConfig] 입니다.
 */
@JvmField
val defaultTlsConfig: TlsConfig = TlsConfig.DEFAULT

/**
 * [TlsConfig] 를 생성합니다.
 *
 * @param initializer [TlsConfig.Builder] 초기화 람다
 * @return [TlsConfig] 인스턴스
 */
inline fun tlsConfig(
    initializer: TlsConfig.Builder.() -> Unit,
): TlsConfig =
    TlsConfig.custom().apply(initializer).build()

/**
 * [TlsConfig] 를 생성합니다.
 *
 * @param supportedProtocols [TLS] 지원 프로토콜
 * @param handshakeTimeout [Timeout] 핸드쉐이크 타임아웃
 * @param supportedCipherSuites [Array]<String>? 지원 암호화 스위트
 * @param versionPolicy [HttpVersionPolicy]? HTTP 버전 정책
 * @param initializer [TlsConfig.Builder] 초기화 람다
 * @return [TlsConfig] 인스턴스
 */
fun tlsConfigOf(
    supportedProtocols: Collection<TLS> = listOf(TLS.V_1_0, TLS.V_1_1, TLS.V_1_2, TLS.V_1_3),
    handshakeTimeout: Timeout? = null,
    supportedCipherSuites: Array<String>? = null,
    versionPolicy: HttpVersionPolicy? = null,
    initializer: TlsConfig.Builder.() -> Unit = {},
): TlsConfig = tlsConfig {
    setSupportedProtocols(*supportedProtocols.toTypedArray())

    handshakeTimeout?.run { setHandshakeTimeout(handshakeTimeout) }
    supportedCipherSuites?.run { setSupportedCipherSuites(*supportedCipherSuites) }
    versionPolicy?.run { setVersionPolicy(versionPolicy) }

    initializer()
}
