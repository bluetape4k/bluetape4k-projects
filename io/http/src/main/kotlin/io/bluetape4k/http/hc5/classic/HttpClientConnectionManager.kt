package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/**
 * 기본 [HttpClientConnectionManager] instance
 */
@JvmField
val defaultHttpClientConnectionManager: PoolingHttpClientConnectionManager =
    PoolingHttpClientConnectionManagerBuilder.create().build()

/**
 * Apache HttpComponent 5 의 [HttpClientConnectionManager]를 빌드합니다.
 *
 * ```
 * val cm = httpClientConnectionManager {
 *      setMaxConnPerRoute(5)
 *      setMaxConnTotal(5)
 * }
 * val httpClient = httpClient { setConnectionManager(cm) }
 * ```
 *
 * @param initializer [PoolingHttpClientConnectionManagerBuilder] 를 초기화하는 람다 함수
 * @return [HttpClientConnectionManager] instance
 */
inline fun httpClientConnectionManager(
    initializer: PoolingHttpClientConnectionManagerBuilder.() -> Unit,
): PoolingHttpClientConnectionManager {
    return PoolingHttpClientConnectionManagerBuilder.create().apply(initializer).build()
}
