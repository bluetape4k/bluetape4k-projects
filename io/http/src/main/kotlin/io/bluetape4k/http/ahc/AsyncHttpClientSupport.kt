@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.http.ahc

import io.bluetape4k.netty.isPresentNettyTransportNativeEpoll
import io.bluetape4k.netty.isPresentNettyTransportNativeKQueue
import io.bluetape4k.utils.Systemx
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.AsyncHttpClientConfig
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl
import org.asynchttpclient.filter.RequestFilter
import org.asynchttpclient.filter.ResponseFilter

/**
 * 런타임/OS가 지원하면 Netty native transport 사용을 활성화합니다.
 *
 * ```kotlin
 * val config = DefaultAsyncHttpClientConfig.Builder()
 *     .applyNativeTransport()
 *     .build()
 * // Linux이면 Epoll, macOS이면 KQueue native transport가 활성화됩니다.
 * ```
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
 * [defaultAsyncHttpClient]에서 사용하는 기본 설정입니다.
 *
 * 참고: 고동시성 환경에서는 OS의 open-file 제한 상향이 필요할 수 있습니다.
 *
 * ```kotlin
 * val client = Dsl.asyncHttpClient(defaultAsyncHttpClientConfig)
 * // compression, redirect, keep-alive, retry 등 기본값이 적용된 클라이언트
 * ```
 */
val defaultAsyncHttpClientConfig: DefaultAsyncHttpClientConfig by lazy {
    DefaultAsyncHttpClientConfig.Builder()
        .applyNativeTransport()   // Netty native transport를 사용할 수 있으면 사용하도록 한다
        .build()
}

/**
 * 공용 기본 [AsyncHttpClient] 인스턴스입니다.
 *
 * ```kotlin
 * val response = defaultAsyncHttpClient
 *     .prepareGet("https://example.com")
 *     .execute()
 *     .get()
 * // response.statusCode == 200
 * ```
 */
val defaultAsyncHttpClient: AsyncHttpClient by lazy {
    Dsl.asyncHttpClient(defaultAsyncHttpClientConfig)
}

/**
 * 기본 권장값과 native transport 감지를 적용해 [DefaultAsyncHttpClientConfig]를 생성합니다.
 *
 * 기본값에는 compression, redirect, keep-alive, retry, socket 옵션이 포함됩니다.
 *
 * ```kotlin
 * val config = asyncHttpClientConfig {
 *     setMaxConnections(100)
 *     setRequestTimeout(5_000)
 * }
 * val client = Dsl.asyncHttpClient(config)
 * ```
 *
 * @param builder 기본값 적용 후 추가 커스터마이징 블록
 */
inline fun asyncHttpClientConfig(
    builder: DefaultAsyncHttpClientConfig.Builder.() -> Unit,
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

/**
 * request/response filter를 등록한 설정을 생성합니다.
 *
 * ```kotlin
 * val filter = AttachHeaderRequestFilter(mapOf("X-App-Version" to "1.0"))
 * val config = asyncHttpClientConfigOf(requestFilters = listOf(filter))
 * val client = Dsl.asyncHttpClient(config)
 * ```
 *
 * @param requestFilters 등록할 [RequestFilter] 목록
 * @param responseFilters 등록할 [ResponseFilter] 목록
 * @return [DefaultAsyncHttpClientConfig] 인스턴스
 */
fun asyncHttpClientConfigOf(
    requestFilters: Collection<RequestFilter> = emptyList(),
    responseFilters: Collection<ResponseFilter> = emptyList(),
): DefaultAsyncHttpClientConfig =
    asyncHttpClientConfig {
        requestFilters.forEach { addRequestFilter(it) }
        responseFilters.forEach { addResponseFilter(it) }
    }

/**
 * [asyncHttpClientConfig]의 기본값을 적용하고 [builder]로 덮어쓴 뒤 [AsyncHttpClient]를 생성합니다.
 *
 * @param builder [DefaultAsyncHttpClientConfig.Builder] 커스터마이징 블록
 * @return 새 [AsyncHttpClient] 인스턴스
 */
inline fun asyncHttpClient(
    builder: DefaultAsyncHttpClientConfig.Builder.() -> Unit,
): AsyncHttpClient {
    return Dsl.asyncHttpClient(asyncHttpClientConfig(builder))
}

/**
 * [config]를 사용해 [AsyncHttpClient]를 생성합니다.
 *
 * ```kotlin
 * val client = asyncHttpClientOf(defaultAsyncHttpClientConfig)
 * val response = client.prepareGet("https://example.com").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param config [AsyncHttpClientConfig] 설정
 * @return [AsyncHttpClient] 인스턴스
 */
inline fun asyncHttpClientOf(
    config: AsyncHttpClientConfig = defaultAsyncHttpClientConfig,
): AsyncHttpClient {
    return Dsl.asyncHttpClient(config)
}

/**
 * 지정한 request filter를 등록한 [AsyncHttpClient]를 생성합니다.
 *
 * ```kotlin
 * val filter = AttachHeaderRequestFilter(mapOf("Authorization" to "Bearer token"))
 * val client = asyncHttpClientOf(filter)
 * val response = client.prepareGet("https://api.example.com/data").execute().get()
 * // response.statusCode == 200
 * ```
 *
 * @param requestFilters 등록할 [RequestFilter] 가변 인수
 * @return [AsyncHttpClient] 인스턴스
 */
inline fun asyncHttpClientOf(vararg requestFilters: RequestFilter): AsyncHttpClient {
    val config = asyncHttpClientConfigOf(requestFilters.toList())
    return asyncHttpClientOf(config)
}
