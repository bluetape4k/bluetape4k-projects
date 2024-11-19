package io.bluetape4k.retrofit2

import com.fasterxml.jackson.databind.json.JsonMapper
import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.result.ResultCallAdapterFactory
import io.bluetape4k.support.classIsPresent
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.reflect.KClass

private val log by lazy { KotlinLogging.logger { } }

/**
 * [ScalarsConverterFactory]의 가본 값
 */
@JvmField
val defaultScalarsConverterFactory = ScalarsConverterFactory.create()

/**
 * [JacksonConverterFactory]의 가본 값
 */
@JvmField
val defaultJsonConverterFactory: retrofit2.Converter.Factory = jacksonConverterFactoryOf()

/**
 * [JacksonConverterFactory]를 생성합니다.
 *
 * ```
 * val jacksonConverterFactory = jacksonConverterFactoryOf(ObjectMapper())
 * ```
 *
 * @param mapper Jackson [JsonMapper]
 * @return [JacksonConverterFactory] 인스턴스
 */
fun jacksonConverterFactoryOf(
    mapper: JsonMapper = Jackson.defaultJsonMapper,
): retrofit2.Converter.Factory =
    JacksonConverterFactory.create(mapper)

/**
 * [Retrofit.Builder]를 생성합니다.
 *
 * ```
 * val retrofitBuilder = retrofitBuilder {
 *         baseUrl("https://api.github.com")
 *         callFactory(okhttp3.OkHttpClient())
 *         addCallAdapterFactory(ResultCallAdapterFactory())
 *         addCallAdapterFactory(ReactorCallAdapterFactory.create())
 *         addConverterFactory(defaultJsonConverterFactory)
 *         addConverterFactory(defaultScalarsConverterFactory)
 * }
 * ```
 *
 * @param initialize [Retrofit.Builder] 초기화 람다
 * @return [Retrofit.Builder] 인스턴스
 */
inline fun retrofitBuilder(
    initialize: Retrofit.Builder.() -> Unit,
): Retrofit.Builder =
    Retrofit.Builder().apply(initialize)

/**
 * [Retrofit.Builder]를 생성합니다.
 *
 * ```
 * val retrofitBuilder = retrofitBuilderOf("https://api.github.com", defaultJsonConverterFactory) {
 *   callFactory(okhttp3.OkHttpClient())
 *   addCallAdapterFactory(ResultCallAdapterFactory())
 *   addCallAdapterFactory(ReactorCallAdapterFactory.create())
 * }
 * val retrofit = retrofitBuilder.build()
 * ```
 *
 * @param baseUrl 기본 URL
 * @param converterFactory [retrofit2.Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param initialize [Retrofit.Builder] 초기화 람다
 * @return [Retrofit.Builder] 인스턴스
 */
inline fun retrofitBuilderOf(
    baseUrl: String = "",
    converterFactory: retrofit2.Converter.Factory = defaultScalarsConverterFactory,
    initialize: Retrofit.Builder.() -> Unit = {},
): Retrofit.Builder =
    retrofitBuilder {
        if (baseUrl.isNotBlank()) {
            baseUrl(baseUrl)
        }
        addConverterFactory(converterFactory)
        if (converterFactory != defaultJsonConverterFactory) {
            addConverterFactory(defaultJsonConverterFactory)
        }

        initialize()
    }

/**
 * [Retrofit]을 생성합니다.
 *
 * ```
 * val retrofit = retrofit("https://api.github.com", defaultJsonConverterFactory) {
 *    callFactory(okhttp3.OkHttpClient())
 *    addCallAdapterFactory(ResultCallAdapterFactory())
 *    addCallAdapterFactory(ReactorCallAdapterFactory.create())
 * }
 * ```
 *
 * @param baseUrl 기본 URL
 * @param converterFactory [retrofit2.Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param initialize [Retrofit.Builder] 초기화 람다
 * @return [Retrofit] 인스턴스
 */
inline fun retrofit(
    baseUrl: String = "",
    converterFactory: retrofit2.Converter.Factory = defaultScalarsConverterFactory,
    initialize: Retrofit.Builder.() -> Unit,
): Retrofit {
    return retrofitBuilderOf(baseUrl, converterFactory, initialize).build()
}

/**
 * [Retrofit]을 생성합니다.
 *
 * ```
 * val retrofit = retrofitOf("https://api.github.com", okhttp3.OkHttpClient(), defaultJsonConverterFactory) {
 *    addCallAdapterFactory(ResultCallAdapterFactory())
 *    addCallAdapterFactory(ReactorCallAdapterFactory.create())
 * }
 * ```
 *
 * @param baseUrl 기본 URL
 * @param callFactory [okhttp3.Call.Factory] (기본값: [okhttp3.OkHttpClient()])
 * @param converterFactory [retrofit2.Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param callAdapterFactories [retrofit2.CallAdapter.Factory] (기본값: [ResultCallAdapterFactory()])
 * @return [Retrofit] 인스턴스
 */
fun retrofitOf(
    baseUrl: String = "",
    callFactory: okhttp3.Call.Factory = okhttp3.OkHttpClient(),
    converterFactory: retrofit2.Converter.Factory = defaultScalarsConverterFactory,
    vararg callAdapterFactories: retrofit2.CallAdapter.Factory = arrayOf(ResultCallAdapterFactory()),
): Retrofit {
    log.debug { "Create Retrofit. baseUrl=$baseUrl, callFactory=${callFactory.javaClass.simpleName}" }
    return retrofit(baseUrl, converterFactory) {
        callFactory(callFactory)

        addCallAdapterFactory(ResultCallAdapterFactory())
        callAdapterFactories.forEach { addCallAdapterFactory(it) }

        if (isPresentRetrofitAdapterRxJava2()) {
            addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        }
        if (isPresentRetrofitAdapterRxJava3()) {
            addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        }
        if (isPresentRetrofitAdapterReactor()) {
            addCallAdapterFactory(ReactorCallAdapterFactory.create())
        }
    }
}

internal fun isPresentRetrofitAdapterRxJava2(): Boolean =
    classIsPresent("retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory")

internal fun isPresentRetrofitAdapterRxJava3(): Boolean =
    classIsPresent("retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory")

internal fun isPresentRetrofitAdapterReactor(): Boolean =
    classIsPresent("com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory")

/**
 * [Retrofit] 서비스를 생성합니다.
 *
 * ```
 * val service = retrofit.service(GitHubService::class.java)
 * ```
 */
fun <T: Any> Retrofit.service(serviceClass: Class<T>): T = create(serviceClass)

/**
 * [Retrofit] 서비스를 생성합니다.
 *
 * ```
 * val service = retrofit.service(GitHubService::class)
 * ```
 */
fun <T: Any> Retrofit.service(serviceClass: KClass<T>): T = create(serviceClass.java)

/**
 * [Retrofit] 서비스를 생성합니다.
 *
 * ```
 * val service = retrofit.service<GitHubService>()
 * ```
 */
inline fun <reified T: Any> Retrofit.service(): T = create(T::class.java)
