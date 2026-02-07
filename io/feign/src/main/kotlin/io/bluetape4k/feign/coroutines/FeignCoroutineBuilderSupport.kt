package io.bluetape4k.feign.coroutines

import feign.AsyncClient
import feign.Request
import feign.Target
import feign.codec.Decoder
import feign.codec.Encoder
import feign.hc5.ApacheHttp5Client
import feign.kotlin.CoroutineFeign
import io.bluetape4k.feign.defaultRequestOptions
import java.util.concurrent.Executors

/**
 * Coroutine 용 Feign Builder 를 생성합니다.
 *
 * ```
 * val coroutineFeignBuilder = coroutineFeignBuilder<HttpbinApi> {
 *     client(AsyncApacheHttp5Client(httpAsyncClientOf()))
 *     encoder(Encoder.Default())
 *     decoder(Decoder.Default())
 *     options(Request.Options())
 *     logLevel(feign.Logger.Level.BASIC)
 *     target("https://nghttp2.org/httpbin")
 * }
 * val api = coroutineFeignBuilder.target<HttpbinApi>()
 * ```
 *
 * @param C Context Type
 * @param builder CoroutineFeign.CoroutineBuilder 초기화 블럭
 * @receiver CoroutineFeign.CoroutineBuilder
 * @return [CoroutineFeign.CoroutineBuilder] instance
 */
inline fun <C: Any> coroutineFeignBuilder(
    @BuilderInference builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit,
): CoroutineFeign.CoroutineBuilder<C> {
    return CoroutineFeign.CoroutineBuilder<C>().apply(builder)
}

/**
 * Coroutine 용 Feign Builder 를 생성합니다.
 *
 * ```
 * val coroutineFeignBuilder = coroutineFeignBuilder {
 *         client(AsyncApacheHttp5Client(httpAsyncClientOf()))
 *         logger(Slf4jLogger(javaClass))
 *         logLevel(Logger.Level.FULL)
 * }
 * val api = coroutineFeignBuilder.target<HttpbinApi>("https://nghttp2.org/httpbin")
 * ```
 *
 * @param C Context Type
 * @param asyncClient AsyncClient instance
 * @param encoder Encoder instance (default: Encoder.Default())
 * @param decoder Decoder instance (default: Decoder.Default())
 * @param opptions Request.Options instance (default: [defaultRequestOptions])
 * @param logLevel feign.Logger.Level instance (default: feign.Logger.Level.BASIC)
 * @param builder CoroutineFeign.CoroutineBuilder 초기화 블럭
 *
 * @see [AsyncClient.Default]
 * @see [io.bluetape4k.http.hc5.async.httpAsyncClientOf]
 */
inline fun <C: Any> coroutineFeignBuilderOf(
    asyncClient: AsyncClient<C> = AsyncClient.Default(ApacheHttp5Client(), Executors.newVirtualThreadPerTaskExecutor()),
    encoder: Encoder = Encoder.Default(),
    decoder: Decoder = Decoder.Default(),
    opptions: Request.Options = defaultRequestOptions,
    logLevel: feign.Logger.Level = feign.Logger.Level.BASIC,
    @BuilderInference builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit = {},
): CoroutineFeign.CoroutineBuilder<C> {
    return coroutineFeignBuilder {
        client(asyncClient)
        encoder(encoder)
        decoder(decoder)
        options(opptions)
        logLevel(logLevel)

        builder()
    }
}

/**
 * Feign 용 Client 를 생성합니다.
 *
 * ```
 * val api = coroutineFeignBuilder<HttpbinApi> {
 *    client<HttpbinApi>("https://nghttp2.org/httpbin")
 * }
 * ```
 *
 * @param T Client type
 * @param baseUrl Base URL
 * @return Feign Client instance
 */
inline fun <reified T: Any> CoroutineFeign.CoroutineBuilder<*>.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else -> target(T::class.java, baseUrl)
}
