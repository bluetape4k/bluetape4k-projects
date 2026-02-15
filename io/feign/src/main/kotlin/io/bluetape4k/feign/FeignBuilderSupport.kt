package io.bluetape4k.feign

import feign.Feign
import feign.Request
import feign.Target
import feign.codec.Decoder
import feign.codec.Encoder

/**
 * 기본 Encoder/Decoder를 적용한 [Feign.Builder]를 생성합니다.
 *
 * ```
 * val feignBuilder = feignBuilder {
 *      client(VertxHttpClient())
 * }
 * val api = feignBuilder.target<HttpbinApi>("https://nghttp2.org/httpbin")
 * ```
 *
 * @param builder [Feign.Builder]를 초기화하는 함수
 * @return 설정 가능한 [Feign.Builder] 인스턴스
 */
inline fun feignBuilder(
    @BuilderInference builder: Feign.Builder.() -> Unit,
): Feign.Builder {
    return Feign.Builder()
        .encoder(Encoder.Default())
        .decoder(Decoder.Default())
        .apply(builder)
}

/**
 * 공통 옵션이 적용된 [Feign.Builder]를 생성합니다.
 *
 * ```
 * val feignBuilder = feignBuilderOf(client=VertxHttpClient())
 * val api = feignBuilder.target<HttpbinApi>("https://nghttp2.org/httpbin")
 * ```
 *
 * @param client  [feign.Client] 인스턴스
 * @param encoder [Encoder] 인스턴스
 * @param decoder [Decoder] 인스턴스
 * @param options 요청 옵션 정보
 * @param logLevel Feign 로깅 레벨
 * @return 공통 설정이 적용된 [Feign.Builder]
 */
fun feignBuilderOf(
    client: feign.Client,
    encoder: Encoder = Encoder.Default(),
    decoder: Decoder = Decoder.Default(),
    options: Request.Options = defaultRequestOptions,
    logLevel: feign.Logger.Level = feign.Logger.Level.BASIC,
): Feign.Builder {
    return feignBuilder {
        client(client)
        encoder(encoder)
        decoder(decoder)
        options(options)
        logLevel(logLevel)
    }
}

/**
 * [feignBuilderOf]의 오탈자 호환 함수입니다.
 */
@Deprecated(
    message = "Use feignBuilderOf()",
    replaceWith = ReplaceWith("feignBuilderOf(client, encoder, decoder, options, logLevel)")
)
fun feingBuilderOf(
    client: feign.Client,
    encoder: Encoder = Encoder.Default(),
    decoder: Decoder = Decoder.Default(),
    options: Request.Options = defaultRequestOptions,
    logLevel: feign.Logger.Level = feign.Logger.Level.BASIC,
): Feign.Builder = feignBuilderOf(
    client = client,
    encoder = encoder,
    decoder = decoder,
    options = options,
    logLevel = logLevel
)

/**
 * 지정한 타입의 Feign 클라이언트를 생성합니다.
 *
 * 동적 URL을 사용하려면 인터페이스의 첫 번째 인자로 [java.net.URI]를 선언하고,
 * 이 함수 호출 시 `baseUrl`을 생략하면 됩니다.
 *
 * ```
 * interface GitHub {
 *   // host 값을 동적 url 로 사용합니다.
 *   // issue 는 RequestBody 로 전달됩니다
 *   @RequestLine("POST /repos/{owner}/{repo}/issues")
 *   fun createIssue(
 *      host:URI,
 *      issue: Issue,
 *      @Param("owner") owner String,
 *      @Param("repo") String repo
 *   ): Unit
 * }
 *
 * // Feign Client 를 생성합니다.
 * val client:GitHub = feignBuilderOf(client=ApacheHttp5C).target<GitHub>()
 * ```
 * @param baseUrl 서비스 기본 URL
 * @return 타입 [T]의 API 클라이언트
 */
inline fun <reified T: Any> Feign.Builder.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else -> target(T::class.java, baseUrl)
}
