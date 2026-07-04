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
 * Wraps `Call<T>` as `Call<Result<T>>` so HTTP and network failures are returned as [Result.failure].
 *
 * ## Contract
 * - Successful HTTP responses (`2xx`) become `Result.success(body)`.
 * - Unsuccessful HTTP responses (`4xx/5xx`) become `Result.failure(HttpException(response))`.
 * - Unsuccessful response error bodies are closed before the failure result is returned.
 * - Delegate callback failures are delivered through `Callback.onResponse` as `Response.success(Result.failure(...))`.
 * - A successful HTTP response with a `null` body becomes an [IOException] failure.
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
         * Creates a [ResultCall] wrapper for [delegate].
         *
         * ## Contract
         * - Throws [IllegalStateException] when [delegate] is already cancelled.
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
     * Executes the delegate synchronously and returns a [Result] response.
     *
     * ## Contract
     * - Exceptions from [delegate.execute] become failure results with `IOException(cause)`.
     * - HTTP failures are wrapped as `Result.failure` instead of being thrown.
     * - HTTP failure error bodies are closed after [HttpException] creation.
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
                    val result = response.toHttpFailureResult<T>()
                    Response.success(result)
                }
            }
        } catch (e: Throwable) {
            val result = Result.failure<T>(IOException(e))
            Response.success(result)
        }
    }

    /**
     * Executes the delegate asynchronously and delivers a [Result] response to [callback].
     *
     * ## Contract
     * - Delegate `onFailure` is converted to `callback.onResponse(...Result.failure...)`.
     * - HTTP failures are wrapped as `Result.failure` and close their error bodies before callback delivery.
     * - Callers branch on `Result.isFailure` instead of Retrofit `onFailure`.
     *
     * ```kotlin
     * resultCall.enqueue(callback)
     * // callback.onResponse receives either Result.success or Result.failure
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
                        val result = response.toHttpFailureResult<T>()
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

    private fun <R> Response<*>.toHttpFailureResult(): Result<R> {
        val exception = HttpException(this)
        closeErrorBodySuppressing(exception)
        return Result.failure(exception)
    }

    private fun Response<*>.closeErrorBodySuppressing(primaryFailure: Throwable) {
        try {
            errorBody()?.close()
        } catch (closeFailure: Throwable) {
            primaryFailure.addSuppressed(closeFailure)
        }
    }
}
