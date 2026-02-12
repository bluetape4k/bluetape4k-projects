@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.http.ahc

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.netty.isPresentNettyTransportNativeEpoll
import io.bluetape4k.netty.isPresentNettyTransportNativeKQueue
import io.bluetape4k.support.unsafeLazy
import io.bluetape4k.utils.Systemx
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.AsyncHttpClientConfig
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.filter.RequestFilter
import org.asynchttpclient.filter.ResponseFilter

/**
 * Netty native transport를 사용할 수 있으면 사용하도록 한다
 */
fun DefaultAsyncHttpClientConfig.Builder.applyNativeTransport() = apply {
    if (Systemx.isUnix && isPresentNettyTransportNativeEpoll()) {
        // setEventLoopGroup(EpollEventLoopGroup())
        setUseNativeTransport(true)

    } else if (Systemx.isMac && isPresentNettyTransportNativeKQueue()) {
        // setEventLoopGroup(KQueueEventLoopGroup())
        setUseNativeTransport(true)
    }
}

/**
 * // NOTE: 비동기 방식에서는 OS 차원에서 open file 제한을 늘려야 합니다.
 * 참고: https://gist.github.com/tombigel/d503800a282fcadbee14b537735d202c
 */
val defaultAsyncHttpClientConfig: DefaultAsyncHttpClientConfig by lazy {
    DefaultAsyncHttpClientConfig.Builder()
        .applyNativeTransport()   // Netty native transport를 사용할 수 있으면 사용하도록 한다
        .build()
}

/**
 * Default [AsyncHttpClient] instance
 */
val defaultAsyncHttpClient: AsyncHttpClient by unsafeLazy {
    Dsl.asyncHttpClient(defaultAsyncHttpClientConfig)
}

/**
 * [DefaultAsyncHttpClientConfig]를 생성합니다.
 */
inline fun asyncHttpClientConfig(
    @BuilderInference builder: DefaultAsyncHttpClientConfig.Builder.() -> Unit,
): DefaultAsyncHttpClientConfig {
    return DefaultAsyncHttpClientConfig.Builder()
        .apply {
            setCompressionEnforced(true)
            setFollowRedirect(true)
            setKeepAlive(true)
            setMaxRedirects(5)
            setMaxRequestRetry(3)
            setPooledConnectionIdleTimeout(120_000)  // 120 seconds (2 minutes)
            setTcpNoDelay(true)
            setSoReuseAddress(true)
        }
        .applyNativeTransport()  // Netty native transport를 사용할 수 있으면 사용하도록 한다
        .apply(builder)
        .build()
}

fun asyncHttpClientConfigOf(
    requestFilters: Collection<RequestFilter> = emptyList(),
    responseFilters: Collection<ResponseFilter> = emptyList(),
): DefaultAsyncHttpClientConfig =
    asyncHttpClientConfig {
        requestFilters.forEach { addRequestFilter(it) }
        responseFilters.forEach { addResponseFilter(it) }
    }

/**
 * 새로운 [AsyncHttpClient]를 생성합니다.
 *
 * ```
 * val ahc = asyncHttpClient {
 *      setCompressionEnforced(true)
 *      setKeeyAlive(true)
 *      setMaxRedirects(5)
 *      setMaxRequestRetry(3)
 * }
 * ```
 *
 * @param builder methods of [DefaultAsyncHttpClientConfig.Builder] that customize resulting AsyncHttpClient
 * @return [AsyncHttpClient] 인스턴스
 */
inline fun asyncHttpClient(
    @BuilderInference builder: DefaultAsyncHttpClientConfig.Builder.() -> Unit,
): AsyncHttpClient {
    val configBuilder = DefaultAsyncHttpClientConfig.Builder().apply(builder)
    return Dsl.asyncHttpClient(configBuilder)
}


/**
 * RequestFilter들을 등록한 [AsyncHttpClient] 를 제공합니다.
 *
 * @param config [AsyncHttpClientConfig] instance
 * @return [AsyncHttpClient] instance
 */
inline fun asyncHttpClientOf(
    config: AsyncHttpClientConfig = defaultAsyncHttpClientConfig,
): AsyncHttpClient {
    return Dsl.asyncHttpClient(config)
}

/**
 * RequestFilter들을 등록한 [AsyncHttpClient] 를 제공합니다.
 *
 * @param requestFilters request filters
 * @return [AsyncHttpClient] instance
 */
inline fun asyncHttpClientOf(vararg requestFilters: RequestFilter): AsyncHttpClient {
    val config = asyncHttpClientConfigOf(requestFilters.toFastList())
    return asyncHttpClientOf(config)
}
