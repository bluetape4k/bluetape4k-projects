package io.bluetape4k.retrofit2

import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val retryScheduler by lazy {
    Executors.newSingleThreadScheduledExecutor()
}

/**
 * [retrofit2.Call]을 비동기로 실행하고, [CompletableFuture]를 반환합니다.
 *
 * @param T
 * @param cancelHandler 취소 핸들러
 * @receiver
 * @return 비동기 호출 결과
 */
inline fun <T> retrofit2.Call<T>.executeAsync(
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    val promise = CompletableFuture<retrofit2.Response<T>>()

    val callback = object: retrofit2.Callback<T> {
        /**
         * Retrofit2 연동에서 `onResponse` 함수를 제공합니다.
         */
        override fun onResponse(call: retrofit2.Call<T>, response: retrofit2.Response<T>) {
            if (call.isCanceled) {
                val ex = retrofit2.HttpException(response)
                cancelHandler(ex)
                promise.cancel(true)
            } else {
                promise.complete(response)
            }
        }

        /**
         * Retrofit2 연동에서 `onFailure` 함수를 제공합니다.
         */
        override fun onFailure(call: retrofit2.Call<T>, t: Throwable) {
            if (call.isCanceled) {
                cancelHandler(t)
                promise.cancel(true)
            } else {
                promise.completeExceptionally(t)
            }
        }
    }

    enqueue(callback)
    return promise
}

/**
 * Resilience4j [Retry]를 이용하여 Call 비동기 실행을 재시도 할 수 있게 합니다.
 *
 * @param T
 * @param retry Resilience4j [Retry] 인스턴스
 * @param cancelHandler 취소 핸들러
 * @receiver
 * @return 비동기 호출 결과
 */
fun <T> retrofit2.Call<T>.executeAsync(
    retry: Retry,
    cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    return Decorators
        .ofCompletionStage { executeAsync(cancelHandler) }
        .withRetry(retry, retryScheduler)
        .get()
        .toCompletableFuture()
}
