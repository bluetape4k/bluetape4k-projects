package io.bluetape4k.feign

import feign.Feign
import feign.Request
import feign.Target
import feign.codec.Decoder
import feign.codec.Encoder

/**
 * [Feign.Builder]를 생성하는 함수
 *
 * ```
 * val feignBuilder = feignBuilder {
 *      client(VertxHttpClient())
 * }
 * val api = feignBuilder.target<HttpbinApi>("https://nghttp2.org/httpbin")
 * ```
 *
 * @param intializer [Feign.Builder]를 초기화하는 함수
 * @return [feign.Feign.Builder] 인스턴스
 */
inline fun feignBuilder(intializer: Feign.Builder.() -> Unit): Feign.Builder {
    return Feign.Builder()
        .encoder(Encoder.Default())
        .decoder(Decoder.Default())
        .apply(intializer)
}

/**
 * [Feign.Builder] 를 생성합니다.
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
 * @param logLevel 로그 레벨 (기본값: [feign.Logger.Level.BASIC])
 * @return [feign.Feign.Builder] 인스턴스
 */
fun feingBuilderOf(
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
 * 지정한 [T] 타입의 Feign Client 를 생성합니다.
 *
 * 동적인 url 을 사용하기 위해 함수의 첫번째 인자를 [java.net.URI] 를 사용하고, 여기서는 baseUrl 을 지정하지 않으면 됩니다.
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
 * @param T
 * @param baseUrl base url for service
 * @return Service API instance
 */
inline fun <reified T: Any> Feign.Builder.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else                    -> target(T::class.java, baseUrl)
}
