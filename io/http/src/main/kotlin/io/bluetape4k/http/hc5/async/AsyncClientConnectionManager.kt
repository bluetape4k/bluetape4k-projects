package io.bluetape4k.http.hc5.async

import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder

/**
 * 기본 설정의 [PoolingAsyncClientConnectionManager] 입니다.
 */
@JvmField
val defaultAsyncClientConnectionManager: PoolingAsyncClientConnectionManager =
    PoolingAsyncClientConnectionManagerBuilder.create().build()

/**
 * [PoolingAsyncClientConnectionManager]를 생성합니다.
 *
 * ```
 * val asyncClientConnectionManager = asyncClientConnectionManager {
 *    setMaxTotal(100)
 *    setDefaultMaxPerRoute(10)
 *    setValidateAfterInactivity(1000)
 *    setConnectionTimeToLive(10, TimeUnit.MINUTES)
 *    setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(5000).build())
 *    setDefaultConnectionConfig(ConnectionConfig.custom().setBufferSize(8 * 1024).build())
 *    setTlsStrategy(TLS_STRATEGY)
 * }
 * ```
 *
 * @return [PoolingAsyncClientConnectionManager] 인스턴스
 */
inline fun asyncClientConnectionManager(
    initializer: PoolingAsyncClientConnectionManagerBuilder.() -> Unit,
): PoolingAsyncClientConnectionManager =
    PoolingAsyncClientConnectionManagerBuilder
        .create()
        .apply(initializer)
        .build()
