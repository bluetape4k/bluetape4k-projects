package io.bluetape4k.retrofit2.result

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.AbstractRetrofitTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.executeAsync
import io.bluetape4k.retrofit2.retrofitBuilderOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.HttpbinAnythingResponse
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
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
        suspend fun getPost(
            @Path("id") postId: Int,
        ): Result<HttpbinAnythingResponse>

        @GET("/status/{statusCode}")
        suspend fun status(
            @Path("statusCode") statusCode: Int,
        ): Result<HttpbinAnythingResponse>
    }

    private val retrofit =
        retrofitBuilderOf(testBaseUrl)
            .callFactory(vertxCallFactoryOf())
            .addConverterFactory(defaultJsonConverterFactory)
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()

    private val api by lazy { retrofit.service<HttpbinCoroutineResultApi>() }

    @Test
    fun `get posts with result`() =
        runTest {
            api.posts().isSuccess.shouldBeTrue()
        }

    @Test
    fun `get exist post with result`() =
        runTest {
            api.getPost(1).isSuccess.shouldBeTrue()
        }

    @Test
    fun `get no-exists post with result`() =
        runTest {
            val notExists = api.status(404)
            notExists.isFailure.shouldBeTrue()
            notExists.exceptionOrNull().shouldNotBeNull() shouldBeInstanceOf HttpException::class
        }

    @Test
    fun `5xx 응답은 Result failure로 반환된다`() =
        runTest {
            val serverError = api.status(500)
            serverError.isFailure.shouldBeTrue()
            val ex = serverError.exceptionOrNull().shouldNotBeNull()
            ex shouldBeInstanceOf HttpException::class
            (ex as HttpException).code() shouldBeEqualTo 500
        }

    @Test
    fun `403 응답은 Result failure로 반환된다`() =
        runTest {
            val forbidden = api.status(403)
            forbidden.isFailure.shouldBeTrue()
            val ex = forbidden.exceptionOrNull().shouldNotBeNull()
            ex shouldBeInstanceOf HttpException::class
            (ex as HttpException).code() shouldBeEqualTo 403
        }


    interface RawApi {
        @GET("/anything/posts")
        fun posts(): retrofit2.Call<HttpbinAnythingResponse>
    }

    @Test
    fun `executeAsync 비동기 호출은 성공 응답을 반환한다`() =
        runSuspendIO {
            val rawRetrofit =
                retrofitBuilderOf(testBaseUrl)
                    .callFactory(vertxCallFactoryOf())
                    .addConverterFactory(defaultJsonConverterFactory)
                    .build()

            val rawApi = rawRetrofit.service<RawApi>()
            val response = rawApi.posts().executeAsync().await()
            response.isSuccessful.shouldBeTrue()
            response.body().shouldNotBeNull()
        }

    @Test
    fun `취소된 Call을 ResultCall로 감싸면 예외가 발생한다`() {
        val rawRetrofit =
            retrofitBuilderOf(testBaseUrl)
                .callFactory(vertxCallFactoryOf())
                .addConverterFactory(defaultJsonConverterFactory)
                .build()

        val rawApi = rawRetrofit.service<RawApi>()
        val call = rawApi.posts()
        call.cancel()
        call.isCanceled.shouldBeTrue()
        runCatching { ResultCall(call) }.isFailure.shouldBeTrue()
    }

    @Test
    fun `성공 결과는 isFailure가 false이다`() =
        runTest {
            val result = api.getPost(1)
            result.isSuccess.shouldBeTrue()
            result.isFailure.shouldBeFalse()
        }
}
