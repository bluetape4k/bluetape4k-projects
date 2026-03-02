package io.bluetape4k.retrofit2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.result.ResultCallAdapterFactory
import io.bluetape4k.support.classIsPresent
import okhttp3.Call
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.reflect.KClass

private val log by lazy { KotlinLogging.logger { } }
private val retrofitAdapterRxJava2Present by lazy {
    classIsPresent("retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory")
}
private val retrofitAdapterRxJava3Present by lazy {
    classIsPresent("retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory")
}
private val retrofitAdapterReactorPresent by lazy {
    classIsPresent("com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory")
}

/**
 * Retrofit 기본 문자열/원시형 컨버터 팩토리입니다.
 *
 * ## 동작/계약
 * - [ScalarsConverterFactory.create]로 생성한 singleton 인스턴스입니다.
 * - `String`, primitive, boxed type 변환 경로에서 사용됩니다.
 *
 * ```kotlin
 * val factory = defaultScalarsConverterFactory
 * // factory != null
 * ```
 */
@JvmField
val defaultScalarsConverterFactory: ScalarsConverterFactory = ScalarsConverterFactory.create()

/**
 * Retrofit 기본 JSON 컨버터 팩토리입니다.
 *
 * ## 동작/계약
 * - [jacksonConverterFactoryOf] 기본 mapper 경로를 사용합니다.
 * - singleton 인스턴스로 재사용됩니다.
 *
 * ```kotlin
 * val factory = defaultJsonConverterFactory
 * // factory != null
 * ```
 */
@JvmField
val defaultJsonConverterFactory: Converter.Factory = jacksonConverterFactoryOf()

/**
 * 지정한 Jackson mapper로 [JacksonConverterFactory]를 생성합니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [Jackson.defaultJsonMapper]입니다.
 * - 반환된 팩토리는 mapper 설정을 그대로 사용합니다.
 *
 * ```kotlin
 * val factory = jacksonConverterFactoryOf()
 * // factory is JacksonConverterFactory == true
 * ```
 *
 * @param mapper JSON 직렬화/역직렬화에 사용할 Jackson mapper입니다.
 */
fun jacksonConverterFactoryOf(
    mapper: ObjectMapper = Jackson.defaultJsonMapper,
): Converter.Factory =
    JacksonConverterFactory.create(mapper)

/**
 * [Retrofit.Builder]를 생성하고 [builder] DSL을 적용합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 새 [Retrofit.Builder] 인스턴스를 생성합니다.
 * - [builder] 내부 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val b = retrofitBuilder { baseUrl("https://example.com") }
 * // b != null
 * ```
 */
inline fun retrofitBuilder(
    @BuilderInference builder: Retrofit.Builder.() -> Unit,
): Retrofit.Builder =
    Retrofit.Builder().apply(builder)

/**
 * 기본 컨버터를 포함한 [Retrofit.Builder]를 생성합니다.
 *
 * ## 동작/계약
 * - [baseUrl]이 blank가 아니면 `baseUrl(...)`을 설정합니다.
 * - [converterFactory]를 먼저 추가하고, JSON 팩토리가 아니면 [defaultJsonConverterFactory]를 추가합니다.
 * - 마지막에 [builder] DSL을 적용합니다.
 *
 * ```kotlin
 * val b = retrofitBuilderOf("https://example.com")
 * // b != null
 * ```
 *
 * @param baseUrl Retrofit 기본 URL입니다. blank면 설정하지 않습니다.
 * @param converterFactory 우선 적용할 컨버터 팩토리입니다.
 */
inline fun retrofitBuilderOf(
    baseUrl: String = "",
    converterFactory: Converter.Factory = defaultScalarsConverterFactory,
    @BuilderInference builder: Retrofit.Builder.() -> Unit = {},
): Retrofit.Builder =
    retrofitBuilder {
        if (baseUrl.isNotBlank()) {
            baseUrl(baseUrl)
        }
        addConverterFactory(converterFactory)
        if (converterFactory != defaultJsonConverterFactory) {
            addConverterFactory(defaultJsonConverterFactory)
        }

        builder()
    }

/**
 * [Retrofit] 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - [retrofitBuilderOf]로 구성한 builder를 즉시 `build()` 합니다.
 * - [builder]에서 callFactory/callAdapter/converter를 추가로 지정할 수 있습니다.
 *
 * ```kotlin
 * val retrofit = retrofit("https://example.com") { callFactory(OkHttpClient()) }
 * // retrofit.baseUrl().toString() == "https://example.com/"
 * ```
 */
inline fun retrofit(
    baseUrl: String = "",
    converterFactory: Converter.Factory = defaultScalarsConverterFactory,
    @BuilderInference builder: Retrofit.Builder.() -> Unit,
): Retrofit {
    return retrofitBuilderOf(baseUrl, converterFactory, builder).build()
}

/**
 * `Call.Factory`와 어댑터들을 포함한 표준 [Retrofit] 구성을 생성합니다.
 *
 * ## 동작/계약
 * - 기본적으로 [ResultCallAdapterFactory]를 포함합니다.
 * - [callAdapterFactories]는 클래스명 기준으로 중복 제거 후 추가됩니다.
 * - RxJava2/RxJava3/Reactor 어댑터가 클래스패스에 존재하면 자동 추가됩니다(동일 타입 미지정 시).
 *
 * ```kotlin
 * val retrofit = retrofitOf("https://example.com", OkHttpClient(), defaultJsonConverterFactory)
 * // retrofit != null
 * ```
 *
 * @param baseUrl Retrofit 기본 URL입니다. blank면 별도 설정이 필요합니다.
 * @param callFactory HTTP 호출 구현체입니다.
 * @param converterFactory 우선 적용할 컨버터 팩토리입니다.
 * @param callAdapterFactories 추가 CallAdapter 팩토리 목록입니다.
 */
fun retrofitOf(
    baseUrl: String = "",
    callFactory: Call.Factory = OkHttpClient(),
    converterFactory: Converter.Factory = defaultScalarsConverterFactory,
    vararg callAdapterFactories: CallAdapter.Factory,
): Retrofit {
    log.debug { "Create Retrofit. baseUrl=$baseUrl, callFactory=${callFactory.javaClass.simpleName}" }
    return retrofit(baseUrl, converterFactory) {
        callFactory(callFactory)

        val adapterFactories = linkedMapOf<String, CallAdapter.Factory>()
        val defaultResultAdapter = ResultCallAdapterFactory()
        adapterFactories[defaultResultAdapter.javaClass.name] = defaultResultAdapter
        callAdapterFactories.forEach { adapterFactories[it.javaClass.name] = it }

        if (isPresentRetrofitAdapterRxJava2() && !adapterFactories.containsKey(RxJava2CallAdapterFactory::class.java.name)) {
            adapterFactories[RxJava2CallAdapterFactory::class.java.name] = RxJava2CallAdapterFactory.create()
        }
        if (isPresentRetrofitAdapterRxJava3() && !adapterFactories.containsKey(RxJava3CallAdapterFactory::class.java.name)) {
            adapterFactories[RxJava3CallAdapterFactory::class.java.name] = RxJava3CallAdapterFactory.create()
        }
        if (isPresentRetrofitAdapterReactor() && !adapterFactories.containsKey(ReactorCallAdapterFactory::class.java.name)) {
            adapterFactories[ReactorCallAdapterFactory::class.java.name] = ReactorCallAdapterFactory.create()
        }

        adapterFactories.values.forEach { addCallAdapterFactory(it) }
    }
}

internal fun isPresentRetrofitAdapterRxJava2(): Boolean =
    retrofitAdapterRxJava2Present

internal fun isPresentRetrofitAdapterRxJava3(): Boolean =
    retrofitAdapterRxJava3Present

internal fun isPresentRetrofitAdapterReactor(): Boolean =
    retrofitAdapterReactorPresent

/**
 * [Retrofit]에서 서비스 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - [Retrofit.create]를 그대로 호출하는 편의 함수입니다.
 * - [serviceClass]는 Retrofit 인터페이스 타입이어야 합니다.
 *
 * ```kotlin
 * val service = retrofit.service(MyApi::class.java)
 * // service != null
 * ```
 */
fun <T: Any> Retrofit.service(serviceClass: Class<T>): T = create(serviceClass)

/**
 * [KClass]를 받아 [Retrofit] 서비스 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [serviceClass.java]를 사용해 [Retrofit.create]를 호출합니다.
 *
 * ```kotlin
 * val service = retrofit.service(MyApi::class)
 * // service != null
 * ```
 */
fun <T: Any> Retrofit.service(serviceClass: KClass<T>): T = create(serviceClass.java)

/**
 * reified 타입으로 [Retrofit] 서비스 인스턴스를 생성합니다.
 *
 * ## 동작/계약
 * - `T::class.java` 경로로 [Retrofit.create]를 호출합니다.
 *
 * ```kotlin
 * val service = retrofit.service<MyApi>()
 * // service != null
 * ```
 */
inline fun <reified T: Any> Retrofit.service(): T = create(T::class.java)
