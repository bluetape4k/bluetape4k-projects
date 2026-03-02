package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * `Call<T>`를 `Call<Result<T>>`로 래핑해 HTTP/네트워크 실패를 `Result.failure`로 전달하는 구현체입니다.
 *
 * ## 동작/계약
 * - 성공 응답(`2xx`)은 `Result.success(body)`로 변환합니다.
 * - 비성공 응답(`4xx/5xx`)은 [HttpException]을 담은 `Result.failure`로 변환합니다.
 * - 예외/실패 경로에서도 `Callback.onResponse`를 호출하며 `Response.success(Result.failure(...))` 형태를 사용합니다.
 * - 응답 본문이 `null`이면 [IOException] 실패로 처리합니다.
 *
 * ```kotlin
 * val resultCall = ResultCall(delegateCall)
 * val result = resultCall.execute().body()!!
 * // result.isSuccess || result.isFailure == true
 * ```
 */
class ResultCall<T> private constructor(
    private val delegate: Call<T>,
): Call<Result<T>> {

    companion object: KLogging() {
        /**
         * [ResultCall] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [delegate]가 이미 취소된 상태면 [IllegalStateException]을 발생시킵니다.
         *
         * ```kotlin
         * val wrapped = ResultCall(delegate)
         * // wrapped.isCanceled() == false
         * ```
         */
        @JvmStatic
        operator fun <T> invoke(delegate: Call<T>): ResultCall<T> {
            if (delegate.isCanceled) {
                error("Call is canceled. delegate=$delegate")
            }
            return ResultCall(delegate)
        }
    }

    /**
     * 동기 호출을 실행하고 [Result] 형태로 반환합니다.
     *
     * ## 동작/계약
     * - 원본 [delegate.execute] 예외는 `IOException(cause)`를 담은 실패 결과로 변환됩니다.
     * - HTTP 실패도 예외를 던지지 않고 `Result.failure`로 감쌉니다.
     *
     * ```kotlin
     * val result = ResultCall(delegate).execute().body()!!
     * // result.isSuccess || result.isFailure == true
     * ```
     */
    override fun execute(): Response<Result<T>> {
        val response: Response<T>
        return try {
            response = delegate.execute()
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    val result = if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(IOException("Response body is null. code=${response.code()}"))
                    }
                    Response.success(response.code(), result)
                }

                else                  -> {
                    val result = Result.failure<T>(HttpException(response))
                    Response.success(result)
                }
            }
        } catch (e: Throwable) {
            val result = Result.failure<T>(IOException(e))
            Response.success(result)
        }
    }

    /**
     * 비동기 호출을 실행하고 [Callback]에 `Result` 형태로 전달합니다.
     *
     * ## 동작/계약
     * - 원본 콜백의 `onFailure`도 최종적으로 `callback.onResponse(...Result.failure...)`로 전달됩니다.
     * - 호출자는 Retrofit `onFailure` 대신 `Result.isFailure`를 확인해 분기할 수 있습니다.
     *
     * ```kotlin
     * resultCall.enqueue(callback)
     * // callback.onResponse에서 Result 성공/실패를 처리
     * ```
     */
    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(toResultCallback(callback))
    }

    private fun toResultCallback(callback: Callback<Result<T>>): Callback<T> {
        log.debug { "Convert to ResultCallback. callback=$callback" }
        return object: Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                when {
                    response.isSuccessful -> {
                        log.trace { "Success! response=$response" }
                        val body = response.body()
                        val result = if (body != null) {
                            Result.success(body)
                        } else {
                            Result.failure(IOException("Response body is null. code=${response.code()}"))
                        }
                        callback.onResponse(this@ResultCall, Response.success(response.code(), result))
                    }

                    else                  -> {
                        log.warn { "Failed to execute call. response=$response" }
                        val result = Result.failure<T>(HttpException(response))
                        callback.onResponse(this@ResultCall, Response.success(result))
                    }
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                log.warn(t) { "Failed to execute call. call=$call" }

                val errorMessage = when (t) {
                    is IOException   -> "Network error"
                    is HttpException -> "Http error"
                    else             -> t.localizedMessage
                }
                val result = Result.failure<T>(IOException(errorMessage, t))
                callback.onResponse(this@ResultCall, Response.success(result))
            }
        }
    }

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun cancel() = delegate.cancel()

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone())
}
