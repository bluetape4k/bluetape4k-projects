package io.bluetape4k.feign.coroutines

import feign.AsyncClient
import feign.DefaultAsyncClient
import feign.Request
import feign.Target
import feign.codec.Decoder
import feign.codec.DefaultDecoder
import feign.codec.DefaultEncoder
import feign.codec.Encoder
import feign.hc5.ApacheHttp5Client
import feign.kotlin.CoroutineFeign
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.feign.defaultRequestOptions


/**
 * 코루틴용 Feign builder를 생성하고 초기화 블록을 적용합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 [CoroutineFeign.CoroutineBuilder]를 생성합니다.
 * - [builder] 설정은 반환 builder에 그대로 반영됩니다.
 *
 * ```kotlin
 * val b = coroutineFeignBuilder<Any> { logLevel(feign.Logger.Level.BASIC) }
 * // b != null
 * ```
 */
inline fun <C: Any> coroutineFeignBuilder(
    builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit,
): CoroutineFeign.CoroutineBuilder<C> {
    return CoroutineFeign.CoroutineBuilder<C>().apply(builder)
}

/**
 * 공통 옵션이 적용된 코루틴용 Feign builder를 생성합니다.
 *
 * ## 동작/계약
 * - [asyncClient], [encoder], [decoder], [options], [logLevel]을 기본 적용합니다.
 * - 이후 [builder]에서 추가 설정/덮어쓰기가 가능합니다.
 * - 기본 [asyncClient]는 virtual thread executor 기반 [AsyncClient.Default]입니다.
 *
 * ```kotlin
 * val b = coroutineFeignBuilderOf<Any>()
 * // b != null
 * ```
 */
inline fun <C: Any> coroutineFeignBuilderOf(
    asyncClient: AsyncClient<C> = DefaultAsyncClient(ApacheHttp5Client(), VirtualThreadExecutor),
    encoder: Encoder = DefaultEncoder(),
    decoder: Decoder = DefaultDecoder(),
    options: Request.Options = defaultRequestOptions,
    logLevel: feign.Logger.Level = feign.Logger.Level.BASIC,
    builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit = {},
): CoroutineFeign.CoroutineBuilder<C> {
    return coroutineFeignBuilder {
        client(asyncClient)
        encoder(encoder)
        decoder(decoder)
        options(options)
        logLevel(logLevel)

        builder()
    }
}

/**
 * 코루틴 Feign builder에서 지정 타입 클라이언트를 생성합니다.
 *
 * ## 동작/계약
 * - [baseUrl]이 blank면 [Target.EmptyTarget]을 사용합니다.
 * - [baseUrl]이 있으면 해당 URL 고정 타겟을 사용합니다.
 *
 * ```kotlin
 * val api = coroutineFeignBuilderOf<Any>().client<MyApi>("https://example.com")
 * // api != null
 * ```
 */
inline fun <reified T: Any> CoroutineFeign.CoroutineBuilder<*>.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else                    -> target(T::class.java, baseUrl)
}
