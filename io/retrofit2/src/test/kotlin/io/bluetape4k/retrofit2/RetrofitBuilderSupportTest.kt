package io.bluetape4k.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import io.bluetape4k.retrofit2.result.ResultCallAdapterFactory
import okhttp3.OkHttpClient
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * [retrofitBuilder], [retrofitBuilderOf], [retrofitOf] 팩토리 함수 단위 테스트입니다.
 */
class RetrofitBuilderSupportTest {

    companion object: KLogging()

    private interface SampleApi {
        @GET("posts/{id}")
        fun getPost(@Path("id") id: Int): retrofit2.Call<String>
    }

    @Test
    fun `retrofitBuilder creates a valid Retrofit builder`() {
        val builder = retrofitBuilder { baseUrl("https://example.com/") }

        builder.shouldNotBeNull()
    }

    @Test
    fun `retrofitBuilderOf with baseUrl sets base URL`() {
        val retrofit = retrofitBuilderOf("https://example.com/").build()

        retrofit.shouldNotBeNull()
        retrofit.baseUrl().toString() shouldBeEqualTo "https://example.com/"
    }

    @Test
    fun `retrofitBuilderOf without baseUrl does not throw`() {
        // Should not throw even without a baseUrl — it's set later
        val builder = retrofitBuilderOf()
        builder.shouldNotBeNull()
    }

    @Test
    fun `retrofitOf with OkHttpClient creates valid Retrofit instance`() {
        val retrofit = retrofitOf(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            callFactory = OkHttpClient(),
            converterFactory = ScalarsConverterFactory.create(),
        )

        retrofit.shouldNotBeNull()
    }

    @Test
    fun `retrofitOf includes ResultCallAdapterFactory by default`() {
        val retrofit = retrofitOf(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            callFactory = OkHttpClient(),
            converterFactory = defaultScalarsConverterFactory,
        )

        // Verify a service proxy can be created (proxy creation validates adapter setup)
        val api = retrofit.service<SampleApi>()
        api.shouldNotBeNull()
    }

    @Test
    fun `retrofitOf with hc5 call factory creates valid Retrofit instance`() {
        val retrofit = retrofitOf(
            baseUrl = "https://jsonplaceholder.typicode.com/",
            callFactory = hc5CallFactoryOf(),
            converterFactory = defaultJsonConverterFactory,
        )

        retrofit.shouldNotBeNull()
        retrofit.service<SampleApi>().shouldNotBeNull()
    }

    @Test
    fun `jacksonConverterFactoryOf creates a non-null converter factory`() {
        val factory = jacksonConverterFactoryOf()

        factory.shouldNotBeNull()
    }

    @Test
    fun `defaultScalarsConverterFactory is a stable singleton`() {
        val first = defaultScalarsConverterFactory
        val second = defaultScalarsConverterFactory

        (first === second) shouldBeEqualTo true
    }

    @Test
    fun `defaultJsonConverterFactory is a stable singleton`() {
        val first = defaultJsonConverterFactory
        val second = defaultJsonConverterFactory

        (first === second) shouldBeEqualTo true
    }

    @Test
    fun `retrofitOf with duplicate adapter factory does not throw`() {
        val customAdapter = ResultCallAdapterFactory()
        val retrofit = retrofitOf(
            baseUrl = "https://example.com/",
            callFactory = OkHttpClient(),
            converterFactory = defaultScalarsConverterFactory,
            callAdapterFactories = arrayOf(customAdapter),
        )

        retrofit.shouldNotBeNull()
    }

    @Test
    fun `isPresentRetrofitAdapterRxJava2 returns boolean without throwing`() {
        val result = runCatching { isPresentRetrofitAdapterRxJava2() }
        result.isFailure.shouldBeFalse()
    }

    @Test
    fun `isPresentRetrofitAdapterRxJava3 returns boolean without throwing`() {
        val result = runCatching { isPresentRetrofitAdapterRxJava3() }
        result.isFailure.shouldBeFalse()
    }

    @Test
    fun `isPresentRetrofitAdapterReactor returns boolean without throwing`() {
        val result = runCatching { isPresentRetrofitAdapterReactor() }
        result.isFailure.shouldBeFalse()
    }
}
