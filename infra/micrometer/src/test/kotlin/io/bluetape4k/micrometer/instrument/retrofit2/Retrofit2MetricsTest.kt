package io.bluetape4k.micrometer.instrument.retrofit2

import com.jakewharton.retrofit2.adapter.reactor.ReactorCallAdapterFactory
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.micrometer.instrument.AbstractMicrometerTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofit
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.suspendExecute
import io.bluetape4k.support.classIsPresent
import io.bluetape4k.testcontainers.http.HttpbinHttp2Server
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.Dispatchers
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
import retrofit2.http.Path
import java.io.Serializable

class Retrofit2MetricsTest: AbstractMicrometerTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    private fun isPresentRetrofitAdapterRxJava2(): Boolean =
        classIsPresent("retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory")

    private fun createRetrofit(factory: CallAdapter.Factory): Retrofit {
        return retrofit(TestService.httpbinBaseUrl) {
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
        val httpbinApi = createRetrofit(factory).service<TestService.HttpbinApi>()

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
        val api = createRetrofit(factory).service<TestService.HttpbinApi>()

        val call = api.getPosts()
        call.shouldNotBeNull()
        val posts = call.suspendExecute().body()
        posts.shouldNotBeNull()
        log.trace { "posts=$posts" }

        repeat(REPEAT_SIZE) {
            api.getPosts().suspendExecute()
        }

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }

    @Test
    fun `measure metrics for coroutine method`() = runSuspendIO {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val api = createRetrofit(factory).service<TestService.HttpbinCoroutineApi>()

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

    @Disabled("MicrometerRetrofitMetricsFactory는 아직 Reactive CallAdapter 와 같이 사용할 수 없습니다")
    @Test
    fun `measure metrics for reactive method`() = runBlocking<Unit> {
        val registry = SimpleMeterRegistry()
        val factory = MicrometerRetrofitMetricsFactory(registry)
        val api = createRetrofit(factory).service<TestService.HttpbinReactiveApi>()

        val posts = api.getPosts().awaitSingle()
        log.debug { "posts=$posts" }

        registry.meters.forEach { meter ->
            log.debug { "id=${meter.id}, tags=${meter.measure().joinToString()}" }
        }
        registry[MicrometerRetrofitMetricsRecorder.METRICS_KEY].timer().shouldNotBeNull()
    }
}


object TestService {

    val httpServer by lazy { HttpbinHttp2Server.Launcher.httpbinHttp2 }

    const val TEST_COUNT = 30
    val httpbinBaseUrl by lazy { httpServer.url }

    interface HttpbinApi {
        @GET("/anything/posts")
        fun getPosts(): Call<HttpbinAnythingResponse>

        @GET("/anything/posts/{id}")
        fun getPost(@Path("id") postId: Int): Call<HttpbinAnythingResponse>
    }

    interface HttpbinCoroutineApi {
        @GET("/anything/posts")
        suspend fun getPosts(): HttpbinAnythingResponse

        @GET("/anything/posts/{id}")
        suspend fun getPost(@Path("id") postId: Int): HttpbinAnythingResponse
    }

    interface HttpbinReactiveApi {
        @GET("/anything/posts")
        fun getPosts(): Mono<HttpbinAnythingResponse>

        @GET("/anything/posts/{id}")
        fun getPost(@Path("id") postId: Int): Mono<HttpbinAnythingResponse>

    }

    /** `/anything`, `/put`, `/patch` 처럼 메소드와 본문 정보를 포함하는 범용 응답입니다. */
    data class HttpbinAnythingResponse(
        val args: Map<String, String>,
        val data: String,
        val files: Map<String, String>,
        val form: Map<String, String>,
        val headers: Map<String, String>,
        val json: Map<String, Any>?,
        val method: String,
        val origin: String,
        val url: String,
    ): Serializable
}
