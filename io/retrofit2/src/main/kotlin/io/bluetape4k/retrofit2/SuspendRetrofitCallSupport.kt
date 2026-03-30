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
 * Retrofit [Call]을 코루틴에서 실행해 [Response]를 반환합니다.
 *
 * ## 동작/계약
 * - 코루틴 취소 시 `Call.cancel()`을 호출합니다.
 * - 호출이 이미 취소된 상태에서 응답/실패가 도착하면 [cancelHandler]를 호출하고 코루틴을 취소합니다.
 * - 네트워크 실패는 예외로 재개(`resumeWithException`)됩니다.
 *
 * ```kotlin
 * val response = api.getPost(1).suspendExecute()
 * // response.isSuccessful == true
 * ```
 *
 * @param cancelHandler 취소 경로에서 호출할 콜백입니다.
 */
suspend inline fun <T> Call<T>.suspendExecute(
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): Response<T> = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation {
        this.cancel()
    }

    val callback = object: Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (call.isCanceled) {
                val ex = HttpException(response)
                cancelHandler(ex)
                cont.cancel(ex)
            } else {
                cont.resume(response) { _, _, _ -> call.cancel() }
            }
        }

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
 * Retrofit [Call]을 코루틴에서 실행하고 Resilience4j [Retry]를 적용합니다.
 *
 * ## 동작/계약
 * - 기본 실행은 [suspendExecute]를 사용합니다.
 * - 재시도마다 현재 [Call]을 `clone()`한 새 인스턴스로 실행합니다.
 * - 재시도 정책은 [retry] 설정(`maxAttempts`, `waitDuration`, retryOnException`)을 그대로 따릅니다.
 * - 취소 경로의 [cancelHandler] 동작은 [suspendExecute]와 동일합니다.
 *
 * ```kotlin
 * val response = api.getPost(1).suspendExecute(retry)
 * // response.isSuccessful == true
 * ```
 *
 * @param retry 적용할 Resilience4j 재시도 정책입니다.
 * @param cancelHandler 취소 경로에서 호출할 콜백입니다.
 */
suspend inline fun <T> Call<T>.suspendExecute(
    retry: Retry,
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): Response<T> {
    return SuspendDecorators
        .ofSupplier { clone().suspendExecute(cancelHandler) }
        .withRetry(retry)
        .get()
}
