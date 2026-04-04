package io.bluetape4k.http.hc5.ssl

import org.apache.hc.client5.http.ssl.HttpsSupport
import javax.net.ssl.HostnameVerifier

/**
 * HTTPS 기본 [HostnameVerifier]입니다.
 *
 * ```kotlin
 * val tlsStrategy = tlsStrategyOf(
 *     hostnameVerifier = defaultHostnameVerifier
 * )
 * // 기본 호스트명 검증기가 적용된 TLS 전략
 * ```
 */
val defaultHostnameVerifier: HostnameVerifier by lazy {
    HttpsSupport.getDefaultHostnameVerifier()
}
