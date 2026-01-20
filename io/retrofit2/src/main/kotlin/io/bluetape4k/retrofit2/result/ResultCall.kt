package io.bluetape4k.retrofit2.result

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import okhttp3.Request
import okio.IOException
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response

/**
 * T 수형의 반환하는 API 를 [Result] 수형으로 감싸서 예외 처리를 유연하게 할 수 있도록 하는 [Call] 입니다.
 *
 * @param T API 의 반환 수형
 * @property delegate 실제 수행할 [Call] 구현체
 */
class ResultCall<T> private constructor(
    private val delegate: Call<T>,
): Call<Result<T>> {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T> invoke(delegate: Call<T>): ResultCall<T> {
            if (delegate.isCanceled) {
                error("Call is canceled. delegate=$delegate")
            }
            return ResultCall(delegate)
        }
    }

    override fun execute(): Response<Result<T>> {
        val response: Response<T>
        return try {
            response = delegate.execute()
            return when {
                response.isSuccessful -> {
                    val result = Result.success(response.body()!!)
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

    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(toResultCallback(callback))
    }

    private fun toResultCallback(callback: Callback<Result<T>>): Callback<T> {
        log.debug { "Convert to ResultCallback. callback=$callback" }
        /**
         * Invoked for a received HTTP response.
         *
         * Note: Http response 에는 application level 의 예외 (404, 500) 이 있을 수 있으므로,
         * 실제 성공 여부는 [Response.isSuccessful] 로 판단해야 합니다.
         */
        return object: Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                when {
                    response.isSuccessful -> {
                        log.trace { "Success! response=$response" }
                        val result = Result.success(response.body()!!)
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
