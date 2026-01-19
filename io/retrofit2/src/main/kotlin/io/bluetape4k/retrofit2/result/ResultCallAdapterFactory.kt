package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
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
 * interface JsonPlaceHolderCoroutineResultApi {
 *     @GET("/posts")
 *     suspend fun posts(): Result<List<Post>>
 *     @GET("/posts/{id}")
 *     suspend fun getPost(@Path("id") postId: Int): Result<Post>
 * }
 *
 * val retrofit2 = retroift2Of("https://api.example.com", callFactory) {
 *  addConverterFactory(defaultJsonConverterFactory)
 *  addCallAdapterFactory(ResultCallAdapterFactory())
 *  // ...
 * }
 *
 * val api = retrofit2.service<JsonPlaceHolderCoroutineResultApi>()
 * val result: Result<Post> = api.getPost(postId=1)
 * ```
 *
 * @see ResultCall
 */
class ResultCallAdapterFactory: CallAdapter.Factory() {

    companion object: KLogging()

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java || returnType !is ParameterizedType) {
            log.warn { "returnType is not Call or ParameterizedType. returnType=$returnType" }
            return null
        }

        // Retrofit2 용 API의 반환 수형이 Kotlin [Result] 수형인지 검사합니다.
        val upperBound = getParameterUpperBound(0, returnType)
        val isResultType = upperBound is ParameterizedType && upperBound.rawType == Result::class.java

        if (isResultType) {
            log.trace { "returnType is Result, create ResultCall ..." }

            return object: CallAdapter<Any, Call<Result<*>>> {
                override fun responseType(): Type {
                    return getParameterUpperBound(0, upperBound)
                }

                @Suppress("UNCHECKED_CAST")
                override fun adapt(call: Call<Any>): Call<Result<*>> {
                    return ResultCall(call) as Call<Result<*>>
                }
            }
        }

        return null
    }
}
