package io.bluetape4k.micrometer.instrument.retrofit2

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.micrometer.instrument.AbstractMicrometerTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.executeAsync
import io.bluetape4k.retrofit2.retrofit
import io.bluetape4k.retrofit2.service
import io.bluetape4k.support.classIsPresent
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.GET
import java.io.Serializable

class Retrofit2MetricsTest: AbstractMicrometerTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private fun isPresentRetrofitAdapterRxJava2(): Boolean =
        classIsPresent("retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory")

    private fun createRetrofit(factory: CallAdapter.Factory): Retrofit {
        return retrofit(TestService.JsonPlaceHolderUrl) {
            callFactory(vertxCallFactoryOf())
            addConverterFactory(defaultJsonConverterFactory)
            addCallAdapterFactory(factory)

            if (isPresentRetrofitAdapterRxJava2()) {
                addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            }
            if (classIsPresent("com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory")) {
                addCallAdapterFactory(ReactorCallAdapterFactory.createAsync())
            }
        }
    }

    @Test
    fun `measure metrics for sync method`() {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val httpbinApi = createRetrofit(factory).service<TestService.JsonPlaceholderApi>()

        val call = httpbinApi.getPosts()
        call.shouldNotBeNull()
        val posts = call.execute().body()
        posts.shouldNotBeNull()
        log.trace { "posts=$posts" }

        repeat(REPEAT_SIZE) {
            httpbinApi.getPosts().execute()
        }

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }

    @Test
    fun `measure metrics for async method`() = runSuspendIO {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val api = createRetrofit(factory).service<TestService.JsonPlaceholderApi>()

        val call = api.getPosts()
        call.shouldNotBeNull()
        val posts = call.executeAsync().await().body()
        posts.shouldNotBeNull()
        log.trace { "posts=$posts" }

        List(REPEAT_SIZE) { api.getPosts().executeAsync() }.sequence().await()

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }

    @Test
    fun `measure metrics for coroutine method`() = runSuspendIO {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val api = createRetrofit(factory).service<TestService.CoroutineJsonPlaceholderApi>()

        val posts = api.getPosts()
        log.trace { "posts=$posts" }

        List(REPEAT_SIZE) {
            launch(Dispatchers.IO) {
                api.getPosts()
            }
        }.joinAll()

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }

    @Disabled("Reactive CallAdapter 와 같이 사용할 수 없습니다")
    @Test
    fun `measure metrics for reactive method`() = runBlocking<Unit> {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val api = createRetrofit(factory).service<TestService.ReactorJsonPlaceholderApi>()

        val posts = api.getPosts().awaitSingle()
        log.debug { "posts=$posts" }

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }
}


object TestService {

    const val TEST_COUNT = 30
    const val JsonPlaceHolderUrl = "https://jsonplaceholder.typicode.com"

    interface JsonPlaceholderApi {
        @GET("/posts")
        fun getPosts(): Call<List<Post>>
    }

    interface CoroutineJsonPlaceholderApi {
        @GET("/posts")
        suspend fun getPosts(): List<Post>
    }

    interface ReactorJsonPlaceholderApi {
        @GET("/posts")
        fun getPosts(): Mono<List<Post>>
    }

    data class Post(
        val userId: Int,
        val id: Int,
        var title: String?,
        var body: String?,
    ): Serializable
}
