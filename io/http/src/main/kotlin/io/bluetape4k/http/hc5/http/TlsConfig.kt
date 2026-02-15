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
 * [TlsConfig]를 생성합니다.
 *
 * @param builder [TlsConfig.Builder] 설정 블록
 * @return [TlsConfig] 인스턴스
 */
inline fun tlsConfig(
    @BuilderInference builder: TlsConfig.Builder.() -> Unit,
): TlsConfig =
    TlsConfig.custom().apply(builder).build()

/**
 * 기본값을 바탕으로 [TlsConfig]를 생성합니다.
 *
 * @param supportedProtocols 지원 [TLS] 프로토콜 목록
 * @param handshakeTimeout TLS 핸드셰이크 타임아웃
 * @param supportedCipherSuites 지원 암호화 스위트 목록
 * @param versionPolicy HTTP 버전 정책
 * @param builder [TlsConfig.Builder] 설정 블록
 * @return [TlsConfig] 인스턴스
 */
inline fun tlsConfigOf(
    supportedProtocols: Collection<TLS> = listOf(TLS.V_1_0, TLS.V_1_1, TLS.V_1_2, TLS.V_1_3),
    handshakeTimeout: Timeout? = null,
    supportedCipherSuites: Array<String>? = null,
    versionPolicy: HttpVersionPolicy? = null,
    @BuilderInference builder: TlsConfig.Builder.() -> Unit = {},
): TlsConfig =
    tlsConfig {
        setSupportedProtocols(*supportedProtocols.toTypedArray())

        handshakeTimeout?.run { setHandshakeTimeout(handshakeTimeout) }
        supportedCipherSuites?.run { setSupportedCipherSuites(*supportedCipherSuites) }
        versionPolicy?.run { setVersionPolicy(versionPolicy) }

        builder()
    }
