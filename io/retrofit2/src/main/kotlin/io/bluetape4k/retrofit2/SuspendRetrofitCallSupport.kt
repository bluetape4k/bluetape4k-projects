package io.bluetape4k.retrofit2

import io.bluetape4k.resilience4j.SuspendDecorators
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.resumeWithException

/**
 * [retrofit2.Call]을 Coroutine 환경에서 실행하고, [Response]를 반환합니다.
 *
 * @param T
 * @param cancelHandler 취소 핸들러
 * @return [Response] 코루틴 호출 결과
 */
suspend inline fun <T> Call<T>.suspendExecute(
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): Response<T> = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {
        this.cancel()
    }

    val callback = object: Callback<T> {
        /**
         * Retrofit2 연동에서 `onResponse` 함수를 제공합니다.
         */
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (call.isCanceled) {
                val ex = HttpException(response)
                cancelHandler(ex)
                cont.cancel(ex)
            } else {
                cont.resume(response) { cause, _, _ -> call.cancel() }
            }
        }

        /**
         * Retrofit2 연동에서 `onFailure` 함수를 제공합니다.
         */
        override fun onFailure(call: Call<T>, t: Throwable) {
            if (call.isCanceled) {
                cancelHandler(t)
                cont.cancel(t)
            } else {
                cont.resumeWithException(t)
            }
        }
    }

    enqueue(callback)
}

/**
 * [retrofit2.Call]을 Coroutine 환경에서 Resilience4j [Retry]를 이용하여 실행하고, [Response]를 반환합니다.
 *
 * @param T
 * @param retry Resilience4j [Retry] 인스턴스
 * @param cancelHandler 취소 핸들러
 * @return [Response] 코루틴 호출 결과
 */
suspend inline fun <T> Call<T>.suspendExecute(
    retry: Retry,
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): Response<T> {
    return SuspendDecorators
        .ofSupplier { suspendExecute(cancelHandler) }
        .withRetry(retry)
        .get()
}
