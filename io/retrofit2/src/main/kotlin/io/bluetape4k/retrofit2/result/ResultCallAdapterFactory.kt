package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 일반적인 수형을 반환하는 API 를 [Result]를 반환하게끔 wrapping 하는
 * [ResultCall]을 생성하는 OkHttp3 [CallAdapter.Factory] 구현체입니다.
 *
 * ```
 * interface HttpbinCoroutineResultApi {
 *     @GET("/anything/posts")
 *     suspend fun posts(): Result<HttpbinAnythingResponse>
 *     @GET("/anything/posts/{id}")
 *     suspend fun getPost(@Path("id") postId: Int): Result<HttpbinAnythingResponse>
 * }
 *
 * val retrofit2 = retrofitOf("https://api.example.com", callFactory) {
 *  addConverterFactory(defaultJsonConverterFactory)
 *  addCallAdapterFactory(ResultCallAdapterFactory())
 *  // ...
 * }
 *
 * val api = retrofit2.service<HttpbinCoroutineResultApi>()
 * val result: Result<HttpbinAnythingResponse> = api.getPost(postId = 1)
 * ```
 *
 * @see ResultCall
 */
class ResultCallAdapterFactory: CallAdapter.Factory() {

    companion object: KLogging()

    /**
     * Retrofit2 연동에서 `get` 함수를 제공합니다.
     */
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java || returnType !is ParameterizedType) {
            return null
        }

        // Retrofit2 용 API의 반환 수형이 Kotlin [Result] 수형인지 검사합니다.
        val upperBound = getParameterUpperBound(0, returnType)
        val isResultType = upperBound is ParameterizedType && upperBound.rawType == Result::class.java

        if (isResultType) {
            log.debug { "returnType is Result, create CallAdapter for Call ..." }

            return object: CallAdapter<Any, Call<Result<*>>> {
                /**
                 * Retrofit2 연동에서 `responseType` 함수를 제공합니다.
                 */
                override fun responseType(): Type {
                    return getParameterUpperBound(0, upperBound)
                }

                /**
                 * Retrofit2 연동에서 `adapt` 함수를 제공합니다.
                 */
                @Suppress("UNCHECKED_CAST")
                override fun adapt(call: Call<Any>): Call<Result<*>> {
                    return ResultCall(call) as Call<Result<*>>
                }
            }
        }

        return null
    }
}
