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
 * 코루틴용 Feign Builder를 생성합니다.
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
 * @param C 컨텍스트 타입
 * @param builder [CoroutineFeign.CoroutineBuilder] 초기화 블록
 * @receiver CoroutineFeign.CoroutineBuilder
 * @return 초기화된 [CoroutineFeign.CoroutineBuilder]
 */
inline fun <C: Any> coroutineFeignBuilder(
    @BuilderInference builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit,
): CoroutineFeign.CoroutineBuilder<C> {
    return CoroutineFeign.CoroutineBuilder<C>().apply(builder)
}

/**
 * 공통 옵션이 적용된 코루틴용 Feign Builder를 생성합니다.
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
 * @param C 컨텍스트 타입
 * @param asyncClient 비동기 Feign 클라이언트
 * @param encoder 요청 인코더
 * @param decoder 응답 디코더
 * @param options 요청 옵션
 * @param logLevel Feign 로깅 레벨
 * @param builder 추가 초기화 블록
 *
 * @see [AsyncClient.Default]
 * @see [io.bluetape4k.http.hc5.async.httpAsyncClientOf]
 */
inline fun <C: Any> coroutineFeignBuilderOf(
    asyncClient: AsyncClient<C> = AsyncClient.Default(ApacheHttp5Client(), Executors.newVirtualThreadPerTaskExecutor()),
    encoder: Encoder = Encoder.Default(),
    decoder: Decoder = Decoder.Default(),
    options: Request.Options = defaultRequestOptions,
    logLevel: feign.Logger.Level = feign.Logger.Level.BASIC,
    @BuilderInference builder: CoroutineFeign.CoroutineBuilder<C>.() -> Unit = {},
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
 * 코루틴용 Feign 클라이언트를 생성합니다.
 *
 * ```
 * val api = coroutineFeignBuilder<HttpbinApi> {
 *    client<HttpbinApi>("https://nghttp2.org/httpbin")
 * }
 * ```
 *
 * @param baseUrl 서비스 기본 URL
 * @return 타입 [T]의 클라이언트
 */
inline fun <reified T: Any> CoroutineFeign.CoroutineBuilder<*>.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else -> target(T::class.java, baseUrl)
}
