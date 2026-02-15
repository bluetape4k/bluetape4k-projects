package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.AbstractRetrofitTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitBuilderOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.HttpbinAnythingResponse
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path

class ResultCallTest: AbstractRetrofitTest() {

    companion object: KLoggingChannel()

    /**
     * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서 Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
     *
     * 여기서는 API 통신을 Coroutines 를 이용합니다.
     */
    interface HttpbinCoroutineResultApi {

        @GET("/anything/posts")
        suspend fun posts(): Result<HttpbinAnythingResponse>

        @GET("/anything/posts/{id}")
        suspend fun getPost(@Path("id") postId: Int): Result<HttpbinAnythingResponse>

        @GET("/status/{statusCode}")
        suspend fun status(@Path("statusCode") statusCode: Int): Result<HttpbinAnythingResponse>

    }

    private val retrofit = retrofitBuilderOf(testBaseUrl)
        .callFactory(vertxCallFactoryOf())
        .addConverterFactory(defaultJsonConverterFactory)
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .build()

    private val api by lazy { retrofit.service<HttpbinCoroutineResultApi>() }

    @Test
    fun `get posts with result`() = runTest {
        api.posts().isSuccess.shouldBeTrue()
    }

    @Test
    fun `get exist post with result`() = runTest {
        api.getPost(1).isSuccess.shouldBeTrue()
    }

    @Test
    fun `get no-exists post with result`() = runTest {
        val notExists = api.status(404)
        notExists.isFailure.shouldBeTrue()
        notExists.exceptionOrNull().shouldNotBeNull() shouldBeInstanceOf HttpException::class
    }
}
