package io.bluetape4k.spring4.retrofit2

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.http.vertx.vertxHttpClientOf
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.micrometer.instrument.retrofit2.MicrometerRetrofitMetricsFactory
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactory
import io.bluetape4k.retrofit2.clients.vertx.VertxCallFactory
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.micrometer.core.instrument.MeterRegistry
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl
import org.asynchttpclient.extras.retrofit.AsyncHttpClientCallFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

/**
 * Retrofit 클라이언트 컨텍스트에서 사용할 기본 빈 구성을 제공한다.
 *
 * ## 동작/계약
 * - 모든 빈은 `@ConditionalOnMissingBean`을 사용해 사용자 정의 빈이 있으면 기본 빈을 대체하지 않는다.
 * - Jackson converter, HTTP call factory, Vert.x client, Micrometer call adapter를 조건부로 등록한다.
 * - 클래스 레벨에서 `Retrofit`과 `JacksonConverterFactory`가 클래스패스에 있을 때만 활성화된다.
 *
 * ```kotlin
 * @Retrofit2Client(
 *     name = "httpbin",
 *     baseUrl = "\${bluetape4k.retrofit2.services.httpbin}",
 *     configuration = [DefaultRetrofitClientConfiguration::class]
 * )
 * interface HttpbinApi
 * ```
 */
@Configuration
@ConditionalOnClass(Retrofit::class, JacksonConverterFactory::class)
class DefaultRetrofitClientConfiguration {
    companion object : KLogging()

    /**
     * `JsonMapper`를 사용하는 Retrofit Jackson 변환기 팩토리를 생성한다.
     *
     * ## 동작/계약
     * - `JsonMapper` 빈이 존재할 때만 등록된다.
     * - 반환값은 `JacksonConverterFactory.create(jsonMapper)` 결과다.
     *
     * ```kotlin
     * val mapper = Jackson.defaultJsonMapper
     * val factory = JacksonConverterFactory.create(mapper)
     * // factory != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JsonMapper::class)
    @ConditionalOnClass(JacksonConverterFactory::class)
    fun jacksonConverterFactory(jsonMapper: JsonMapper): Converter.Factory = JacksonConverterFactory.create(jsonMapper)

    /**
     * 기본 `JsonMapper` 빈을 제공한다.
     *
     * ## 동작/계약
     * - 사용자 정의 `JsonMapper`가 없을 때만 등록된다.
     * - `Jackson.defaultJsonMapper` 인스턴스를 그대로 반환한다.
     *
     * ```kotlin
     * val mapper = Jackson.defaultJsonMapper
     * // mapper != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    fun jsonMapper(): JsonMapper = Jackson.defaultJsonMapper

    /**
     * 주입된 `AsyncHttpClient`를 Retrofit `Call.Factory`로 감싼다.
     *
     * ## 동작/계약
     * - `AsyncHttpClient` 빈과 `AsyncHttpClientCallFactory` 클래스가 모두 있을 때만 등록된다.
     * - 전달받은 `asyncHttpClient`를 supplier로 고정해 call factory를 만든다.
     *
     * ```kotlin
     * val ahc = Dsl.asyncHttpClient()
     * val callFactory = asyncHttpClientCallFactory { httpClientSupplier { ahc } }
     * // callFactory != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AsyncHttpClient::class)
    @ConditionalOnClass(AsyncHttpClientCallFactory::class)
    fun asyncHttpClientCallFactory(asyncHttpClient: AsyncHttpClient): okhttp3.Call.Factory {
        log.debug { "Create AsyncHttpClientCallFactory" }
        return asyncHttpClientCallFactory {
            httpClientSupplier { asyncHttpClient }
        }
    }

    /**
     * 기본 `AsyncHttpClient` 빈을 생성한다.
     *
     * ## 동작/계약
     * - `AsyncHttpClient` 타입 빈이 없고 클래스패스에 타입이 존재할 때만 등록된다.
     * - `Dsl.asyncHttpClient()`로 신규 클라이언트를 생성한다.
     *
     * ```kotlin
     * val client = Dsl.asyncHttpClient()
     * // client != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(AsyncHttpClient::class)
    fun asyncHttpClient(): AsyncHttpClient {
        log.debug { "Create AsyncHttpClient" }
        return Dsl.asyncHttpClient()
    }

    /**
     * Vert.x `HttpClient`를 Retrofit `Call.Factory`로 변환한다.
     *
     * ## 동작/계약
     * - Vert.x `HttpClient` 빈이 존재하고 `VertxCallFactory` 클래스가 있을 때만 등록된다.
     * - `vertxCallFactoryOf(httpClient)` 결과를 그대로 반환한다.
     *
     * ```kotlin
     * val httpClient = vertxHttpClientOf()
     * val callFactory = vertxCallFactoryOf(httpClient)
     * // callFactory != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(io.vertx.core.http.HttpClient::class)
    @ConditionalOnClass(VertxCallFactory::class)
    fun vertxCallFactory(httpClient: io.vertx.core.http.HttpClient): okhttp3.Call.Factory {
        log.debug { "Create VertxCallFactory" }
        return vertxCallFactoryOf(httpClient)
    }

    /**
     * 기본 Vert.x `HttpClient` 빈을 생성한다.
     *
     * ## 동작/계약
     * - Vert.x `HttpClient` 빈이 없고 관련 클래스가 클래스패스에 있을 때만 등록된다.
     * - `vertxHttpClientOf()`로 새 인스턴스를 생성한다.
     *
     * ```kotlin
     * val httpClient = vertxHttpClientOf()
     * // httpClient != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(io.vertx.core.http.HttpClient::class)
    fun vertxHttpClient(): io.vertx.core.http.HttpClient {
        log.debug { "Create Vertx. HttpClient" }
        return vertxHttpClientOf()
    }

    /**
     * Micrometer 기반 Retrofit metrics call adapter factory를 등록한다.
     *
     * ## 동작/계약
     * - `MeterRegistry` 빈과 `MicrometerRetrofitMetricsFactory` 클래스가 있을 때만 등록된다.
     * - 생성된 factory는 Retrofit builder의 call adapter로 추가될 수 있다.
     *
     * ```kotlin
     * val registry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
     * val factory = MicrometerRetrofitMetricsFactory(registry)
     * // factory != null
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry::class)
    @ConditionalOnClass(MicrometerRetrofitMetricsFactory::class)
    fun micrometerRetrofitMetricsFactory(meterRegistry: MeterRegistry): MicrometerRetrofitMetricsFactory =
        MicrometerRetrofitMetricsFactory(meterRegistry)
}
