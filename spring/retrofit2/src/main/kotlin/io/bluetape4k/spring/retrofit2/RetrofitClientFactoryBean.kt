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
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit

class RetrofitClientFactoryBean: FactoryBean<Any?>, ApplicationContextAware, InitializingBean {

    companion object: KLogging()

    var type: Class<*> = uninitialized()
    var name: String = uninitialized()
    var baseUrl: String = uninitialized()
    var ctx: ApplicationContext = uninitialized()

    override fun getObject(): Any? {
        log.debug { "Get Retrofit2Client Service ..." }

        val retrofitClientContext = this.ctx.getBean(RetrofitClientContext::class.java)
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
            return vertxCallFactoryOf()
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
