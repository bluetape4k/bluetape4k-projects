package io.bluetape4k.feign

import feign.Feign
import feign.Request
import feign.Target
import feign.codec.Decoder
import feign.codec.Encoder

/**
 * 기본 encoder/decoder가 적용된 [Feign.Builder]를 생성합니다.
 *
 * ## 동작/계약
 * - 기본값으로 [Encoder.Default], [Decoder.Default]를 설정합니다.
 * - 이후 [builder] 블록에서 설정을 덮어쓸 수 있습니다.
 *
 * ```kotlin
 * val b = feignBuilder { client(VertxHttpClient()) }
 * // b != null
 * ```
 */
inline fun feignBuilder(
    builder: Feign.Builder.() -> Unit,
): Feign.Builder {
    return Feign.Builder()
        .encoder(Encoder.Default())
        .decoder(Decoder.Default())
        .apply(builder)
}

/**
 * 공통 옵션이 적용된 [Feign.Builder]를 생성합니다.
 *
 * ## 동작/계약
 * - [client], [encoder], [decoder], [options], [logLevel]을 순서대로 설정합니다.
 * - 반환된 builder는 추가 설정을 계속 적용할 수 있습니다.
 *
 * ```kotlin
 * val b = feignBuilderOf(client = VertxHttpClient())
 * // b != null
 * ```
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
 * 오탈자 함수명(`feingBuilderOf`) 호환용 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 내부에서 [feignBuilderOf]로 위임합니다.
 * - 신규 코드는 [feignBuilderOf] 사용을 권장합니다.
 *
 * ```kotlin
 * val b = feingBuilderOf(client = VertxHttpClient())
 * // b != null
 * ```
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
 * 지정 타입의 Feign API 클라이언트를 생성합니다.
 *
 * ## 동작/계약
 * - [baseUrl]이 blank면 [Target.EmptyTarget] 기반으로 생성합니다.
 * - [baseUrl]이 있으면 해당 URL을 고정 대상으로 사용합니다.
 *
 * ```kotlin
 * val api = feignBuilderOf(client = VertxHttpClient()).client<MyApi>("https://example.com")
 * // api != null
 * ```
 *
 * @param baseUrl 대상 기본 URL입니다. blank면 동적 URL 타겟을 사용합니다.
 */
inline fun <reified T: Any> Feign.Builder.client(baseUrl: String? = null): T = when {
    baseUrl.isNullOrBlank() -> target(Target.EmptyTarget.create(T::class.java))
    else -> target(T::class.java, baseUrl)
}
