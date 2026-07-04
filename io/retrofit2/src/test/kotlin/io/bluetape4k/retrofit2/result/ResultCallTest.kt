package io.bluetape4k.retrofit2.result

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
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
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ResultCallTest: AbstractRetrofitTest() {
    companion object: KLoggingChannel()

    /**
     * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서 Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
     *
     * 여기서는 API 통신을 Coroutines 를 이용합니다.
     */
    interface HttpbinCoroutineResultApi {
        @GET("anything/posts")
        suspend fun posts(): Result<HttpbinAnythingResponse>

        @GET("anything/posts/{id}")
        suspend fun getPost(
            @Path("id") postId: Int,
        ): Result<HttpbinAnythingResponse>

        @GET("status/{statusCode}")
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
        @GET("anything/posts")
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

    @Test
    fun `execute closes error response body before returning Result failure`() {
        val errorBody = CloseTrackingResponseBody()
        val delegate = ControlledCall<String>(syncResponse = Response.error(404, errorBody))

        val response = ResultCall(delegate).execute()
        val result = response.body().shouldNotBeNull()

        result.isFailure.shouldBeTrue()
        val exception = result.exceptionOrNull().shouldNotBeNull()
        exception shouldBeInstanceOf HttpException::class
        (exception as HttpException).code() shouldBeEqualTo 404
        errorBody.closed.shouldBeTrue()
    }

    @Test
    fun `enqueue closes error response body before returning Result failure`() {
        val errorBody = CloseTrackingResponseBody()
        val delegate = ControlledCall<String>()
        val completed = CompletableFuture<Response<Result<String>>>()

        ResultCall(delegate).enqueue(
            object: Callback<Result<String>> {
                override fun onResponse(
                    call: Call<Result<String>>,
                    response: Response<Result<String>>,
                ) {
                    completed.complete(response)
                }

                override fun onFailure(
                    call: Call<Result<String>>,
                    t: Throwable,
                ) {
                    completed.completeExceptionally(t)
                }
            }
        )

        delegate.callback.onResponse(delegate, Response.error(500, errorBody))

        val result = completed.get(1, TimeUnit.SECONDS).body().shouldNotBeNull()
        result.isFailure.shouldBeTrue()
        val exception = result.exceptionOrNull().shouldNotBeNull()
        exception shouldBeInstanceOf HttpException::class
        (exception as HttpException).code() shouldBeEqualTo 500
        errorBody.closed.shouldBeTrue()
    }

    private class ControlledCall<T>(
        private val syncResponse: Response<T>? = null,
    ): Call<T> {
        lateinit var callback: Callback<T>
            private set

        private var executed = false
        private var canceled = false

        override fun enqueue(callback: Callback<T>) {
            this.callback = callback
            executed = true
        }

        override fun execute(): Response<T> {
            executed = true
            return syncResponse.shouldNotBeNull()
        }

        override fun clone(): Call<T> = ControlledCall(syncResponse)

        override fun isExecuted(): Boolean = executed

        override fun cancel() {
            canceled = true
        }

        override fun isCanceled(): Boolean = canceled

        override fun request(): Request =
            Request.Builder().url("https://example.test/").build()

        override fun timeout(): Timeout = Timeout.NONE
    }

    private class CloseTrackingResponseBody: ResponseBody() {
        private val source = CloseTrackingSource()
        val closed: Boolean get() = source.closed

        override fun contentType() = null

        override fun contentLength(): Long = 0L

        override fun source(): BufferedSource = source.buffer()
    }

    private class CloseTrackingSource: Source {
        var closed: Boolean = false
            private set

        override fun close() {
            closed = true
        }

        override fun read(sink: Buffer, byteCount: Long): Long = -1L

        override fun timeout(): Timeout = Timeout.NONE
    }
}
