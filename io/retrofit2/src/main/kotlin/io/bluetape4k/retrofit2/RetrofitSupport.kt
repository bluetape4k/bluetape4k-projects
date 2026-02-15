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
 * 기본 [ScalarsConverterFactory] 인스턴스입니다.
 */
@JvmField
val defaultScalarsConverterFactory: ScalarsConverterFactory = ScalarsConverterFactory.create()

/**
 * 기본 JSON 컨버터 팩토리([JacksonConverterFactory])입니다.
 */
@JvmField
val defaultJsonConverterFactory: Converter.Factory = jacksonConverterFactoryOf()

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
    mapper: ObjectMapper = Jackson.defaultJsonMapper,
): Converter.Factory =
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
 * @param builder [Retrofit.Builder] 초기화 람다
 * @return [Retrofit.Builder] 인스턴스
 */
inline fun retrofitBuilder(
    @BuilderInference builder: Retrofit.Builder.() -> Unit,
): Retrofit.Builder =
    Retrofit.Builder().apply(builder)

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
 * @param converterFactory [Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param builder [Retrofit.Builder] 초기화 람다
 * @return [Retrofit.Builder] 인스턴스
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
 * @param converterFactory [Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param builder [Retrofit.Builder] 초기화 람다
 * @return [Retrofit] 인스턴스
 */
inline fun retrofit(
    baseUrl: String = "",
    converterFactory: Converter.Factory = defaultScalarsConverterFactory,
    @BuilderInference builder: Retrofit.Builder.() -> Unit,
): Retrofit {
    return retrofitBuilderOf(baseUrl, converterFactory, builder).build()
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
 * @param callFactory [Call.Factory] (기본값: [okhttp3.OkHttpClient()])
 * @param converterFactory [Converter.Factory] (기본값: [defaultScalarsConverterFactory])
 * @param callAdapterFactories 추가 [CallAdapter.Factory] 목록
 * @return [Retrofit] 인스턴스
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

/**
 * RxJava2 Retrofit 어댑터가 클래스패스에 존재하는지 확인합니다.
 */
internal fun isPresentRetrofitAdapterRxJava2(): Boolean =
    retrofitAdapterRxJava2Present

/**
 * RxJava3 Retrofit 어댑터가 클래스패스에 존재하는지 확인합니다.
 */
internal fun isPresentRetrofitAdapterRxJava3(): Boolean =
    retrofitAdapterRxJava3Present

/**
 * Reactor Retrofit 어댑터가 클래스패스에 존재하는지 확인합니다.
 */
internal fun isPresentRetrofitAdapterReactor(): Boolean =
    retrofitAdapterReactorPresent

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
