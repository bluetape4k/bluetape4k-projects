package io.bluetape4k.http.hc5.ssl

import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.ssl.SSLContexts
import javax.net.ssl.SSLContext

/**
 * [initializer]를 이용해 [SSLContextBuilder] 를 빌드합니다.
 *
 * ```
 * val sslContext = sslContext {
 *     loadTrustMaterial(trustStore)
 *     loadKeyMaterial(keyStore, keyPassword)
 *     setProtocol("TLSv1.2")
 * }
 * ```
 *
 * @param initializer [SSLContextBuilder] 초기화 람다
 * @return [SSLContext]
 */
inline fun sslContext(
    initializer: SSLContextBuilder.() -> Unit,
): SSLContext {
    return SSLContexts.custom().apply(initializer).build()
}

/**
 * 기본 [SSLContext] 를 생성합니다.
 *
 * ```
 * val sslContext = sslContextOf()
 * ```
 *
 * @return [SSLContext]
 */
fun sslContextOf(): SSLContext = SSLContexts.createDefault()

/**
 * 시스템 기본 [SSLContext] 를 생성합니다.
 *
 * ```
 * val sslContext = sslContextOfSystem()
 * ```
 *
 * @return [SSLContext]
 */
fun sslContextOfSystem(): SSLContext = SSLContexts.createSystemDefault()
