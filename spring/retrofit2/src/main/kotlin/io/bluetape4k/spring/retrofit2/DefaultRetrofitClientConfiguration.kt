package io.bluetape4k.spring.retrofit2

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

@Configuration
@ConditionalOnClass(Retrofit::class, JacksonConverterFactory::class)
class DefaultRetrofitClientConfiguration {

    companion object: KLogging()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JsonMapper::class)
    @ConditionalOnClass(JacksonConverterFactory::class)
    fun jacksonConverterFactory(jsonMapper: JsonMapper): Converter.Factory {
        return JacksonConverterFactory.create(jsonMapper)
    }

    @Bean
    @ConditionalOnMissingBean
    fun jsonMapper(): JsonMapper {
        return Jackson.defaultJsonMapper
    }

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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(AsyncHttpClient::class)
    fun asyncHttpClient(): AsyncHttpClient {
        log.debug { "Create AsyncHttpClient" }
        return Dsl.asyncHttpClient()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(io.vertx.core.http.HttpClient::class)
    @ConditionalOnClass(VertxCallFactory::class)
    fun vertxCallFactory(httpClient: io.vertx.core.http.HttpClient): okhttp3.Call.Factory {
        log.debug { "Create VertxCallFactory" }
        return vertxCallFactoryOf(httpClient)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(io.vertx.core.http.HttpClient::class)
    fun vertxHttpClient(): io.vertx.core.http.HttpClient {
        log.debug { "Create Vertx. HttpClient" }
        return vertxHttpClientOf()
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry::class)
    @ConditionalOnClass(MicrometerRetrofitMetricsFactory::class)
    fun micrometerRetrofitMetricsFactory(meterRegistry: MeterRegistry): MicrometerRetrofitMetricsFactory {
        return MicrometerRetrofitMetricsFactory(meterRegistry)
    }
}
