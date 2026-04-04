package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * `Call<Result<T>>` 반환 타입을 [ResultCall]로 변환하는 Retrofit [CallAdapter.Factory]입니다.
 *
 * ## 동작/계약
 * - 반환 타입이 `Call<Result<...>>`인 경우에만 어댑터를 생성합니다.
 * - 조건이 맞지 않으면 `null`을 반환해 다른 팩토리에 위임합니다.
 * - 생성된 어댑터는 실제 [Call]을 [ResultCall]로 감싸 예외를 `Result.failure`로 변환합니다.
 *
 * ```kotlin
 * val retrofit = retrofitOf(baseUrl, callFactory, defaultJsonConverterFactory, ResultCallAdapterFactory())
 * val api = retrofit.service<MyResultApi>()
 * // api.someCall() 반환 타입 == Result<...>
 * ```
 */
class ResultCallAdapterFactory: CallAdapter.Factory() {

    companion object: KLogging()

    /**
     * 반환 타입에 맞는 [CallAdapter]를 조회합니다.
     *
     * ## 동작/계약
     * - `Call<Result<T>>`이면 [ResultCall] 어댑터를 반환합니다.
     * - 그 외 타입이면 `null`을 반환합니다.
     *
     * ```kotlin
     * val factory = ResultCallAdapterFactory()
     * // Call<Result<T>> 타입에서만 어댑터 생성
     * ```
     */
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java || returnType !is ParameterizedType) {
            return null
        }

        val upperBound = getParameterUpperBound(0, returnType)
        val isResultType = upperBound is ParameterizedType && upperBound.rawType == Result::class.java

        if (isResultType) {
            log.debug { "returnType is Result, create CallAdapter for Call ..." }

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
