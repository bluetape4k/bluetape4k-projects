package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.config.RequestConfig

/**
 * 새로운 [RequestConfig] 를 생성합니다.
 *
 * ```
 * val requestConfig = requestConfig {
 *    setConnectTimeout(1000)
 *    setSocketTimeout(1000)
 *    // ...
 * }
 * ```
 *
 * @param initializer [RequestConfig.Builder] 를 초기화합니다.
 * @return [RequestConfig] 인스턴스
 */
inline fun requestConfig(
    initializer: RequestConfig.Builder.() -> Unit,
): RequestConfig {
    return RequestConfig.custom().apply(initializer).build()
}

/**
 * 기본 [RequestConfig] 를 생성합니다.
 */
fun requestConfigOf(): RequestConfig = requestConfig {}
