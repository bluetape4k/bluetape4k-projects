package io.bluetape4k.http.hc5.classic

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.HttpClientConnectionManager

/** 기본 [HttpClientConnectionManager] 인스턴스입니다. */
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
 * @param builder [PoolingHttpClientConnectionManagerBuilder] 초기화 람다
 * @return [HttpClientConnectionManager] 인스턴스
 */
inline fun httpClientConnectionManager(
    @BuilderInference builder: PoolingHttpClientConnectionManagerBuilder.() -> Unit,
): PoolingHttpClientConnectionManager =
    PoolingHttpClientConnectionManagerBuilder
        .create()
        .apply(builder)
        .build()
