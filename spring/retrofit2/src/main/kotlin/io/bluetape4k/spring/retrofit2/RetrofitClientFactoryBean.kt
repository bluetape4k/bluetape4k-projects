package io.bluetape4k.spring.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactory
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.bluetape4k.support.classIsPresent
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import io.bluetape4k.support.uninitialized
import okhttp3.Call
import okhttp3.OkHttpClient
import org.asynchttpclient.Dsl
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * `@Retrofit2Client` 메타데이터를 기반으로 Retrofit 서비스 프록시를 생성하는 FactoryBean이다.
 *
 * ## 동작/계약
 * - `baseUrl`과 `type`으로 `Retrofit.Builder`를 생성한 뒤 대상 인터페이스 프록시를 반환한다.
 * - `RetrofitClientContext`에서 이름별 `OkHttpClient` 또는 `Call.Factory`를 조회해 우선 적용한다.
 * - `Call.Factory`가 없으면 Vert.x 클래스 존재 여부를 확인해 Vert.x 또는 AsyncHttpClient 기반 기본 factory를 생성한다.
 * - `afterPropertiesSet()`에서 `ctx`, `type`, `name`, `baseUrl`을 검증하며 공백 값이면 예외를 던진다.
 *
 * ```kotlin
 * val factory = RetrofitClientFactoryBean().apply {
 *     type = HttpbinApi::class.java
 *     name = "httpbin"
 *     baseUrl = "https://localhost/"
 * }
 * // afterPropertiesSet() 이후 getObject()는 HttpbinApi 프록시를 반환한다.
 * ```
 */
class RetrofitClientFactoryBean: FactoryBean<Any?>, ApplicationContextAware, InitializingBean {

    companion object: KLogging()

    /**
     * 생성할 Retrofit 서비스 인터페이스 타입이다.
     *
     * ## 동작/계약
     * - `Retrofit.create(type)`에 전달된다.
     * - 초기화 단계에서 null 여부를 검증한다.
     *
     * ```kotlin
     * factory.type = HttpbinApi::class.java
     * // getObjectType() == HttpbinApi::class.java
     * ```
     */
    var type: Class<*> = uninitialized()
    /**
     * Retrofit 클라이언트 이름이다.
     *
     * ## 동작/계약
     * - `RetrofitClientContext`에서 이름 기반 빈 조회 키로 사용된다.
     * - 초기화 단계에서 공백 문자열 여부를 검증한다.
     *
     * ```kotlin
     * factory.name = "jsonPlaceHolderApi"
     * // named context 조회 키로 사용된다.
     * ```
     */
    var name: String = uninitialized()
    /**
     * Retrofit builder에 전달할 base URL이다.
     *
     * ## 동작/계약
     * - `Retrofit.Builder().baseUrl(baseUrl)`에 그대로 전달된다.
     * - 초기화 단계에서 공백 문자열 여부를 검증한다.
     *
     * ```kotlin
     * factory.baseUrl = "https://jsonplaceholder.typicode.com/"
     * // Retrofit builder baseUrl 설정값이 된다.
     * ```
     */
    var baseUrl: String = uninitialized()
    /**
     * `RetrofitClientContext`를 조회할 Spring `ApplicationContext`다.
     *
     * ## 동작/계약
     * - `setApplicationContext`에서 주입된다.
     * - `getObject()` 호출 시 `RetrofitClientContext` 빈을 조회하는 데 사용된다.
     *
     * ```kotlin
     * factory.setApplicationContext(applicationContext)
     * // factory.ctx 로 context가 보관된다.
     * ```
     */
    var ctx: ApplicationContext = uninitialized()

    override fun getObject(): Any? {
        log.debug { "Get Retrofit2Client Service ..." }

        val retrofitClientContext = this.ctx.getBean<RetrofitClientContext>()
        val retrofitBuilder = Retrofit.Builder().baseUrl(this.baseUrl)

        val client = retrofitClientContext.getInstance(this.name, OkHttpClient::class.java)
        if (client != null) {
            log.info { "Add Call.Factory with OkHttpClient" }
            retrofitBuilder.client(client)
        } else {
            val callFactory = retrofitClientContext.getInstance(this.name, Call.Factory::class.java)
                ?: createDefaultCallFactory()
            log.info { "Add Call.Factory ... $callFactory" }
            retrofitBuilder.callFactory(callFactory)
        }

        // Add Converter.Factory (like Jackson)
        retrofitClientContext.getInstances(this.name, Converter.Factory::class.java)
            ?.forEach { (key, factory) ->
                log.debug { "Add Converter.Factory. key=$key, factory=${factory.javaClass.name}" }
                retrofitBuilder.addConverterFactory(factory)
            }

        // Add Call.Factory (like MicrometerRetrofitMetricsFactory)
        retrofitClientContext.getInstances(this.name, CallAdapter.Factory::class.java)
            ?.forEach { (key, factory) ->
                log.debug { "Add CallAdapter.Factory. key=$key, factory=${factory.javaClass.name}" }
                retrofitBuilder.addCallAdapterFactory(factory)
            }

        return retrofitBuilder.build().create(this.type)
    }

    private fun createDefaultCallFactory(): Call.Factory {
        log.debug { "Try to create DefaultCallFactory." }
        return if (classIsPresent("io.vertx.core.http.HttpClient")) {
            log.debug { "Create Vert.x HttpClient" }
            vertxCallFactoryOf()
        } else {
            log.info { "Add Call.Factory with AsyncHttpClient" }
            val ahc = Dsl.asyncHttpClient()
            asyncHttpClientCallFactory {
                httpClientSupplier { ahc }
            }
        }
    }

    override fun getObjectType(): Class<*> = type

    override fun isSingleton(): Boolean = true

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        log.debug { "Set Application Context ..." }
        this.ctx = applicationContext
    }

    override fun afterPropertiesSet() {
        log.debug { "RetrofitClientFactoryBean Property check ... type=$type, name=$name, baseUrl=$baseUrl" }

        this.ctx.requireNotNull("ctx")
        this.type.requireNotNull("type")
        this.name.requireNotBlank("name")
        this.baseUrl.requireNotBlank("baseUrl")
    }
}
