package io.bluetape4k.retrofit2

import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

inline fun <T> retrofit2.Call<T>.executeAsync(
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    val promise = CompletableFuture<retrofit2.Response<T>>()

    val callback = object: retrofit2.Callback<T> {
        override fun onResponse(call: retrofit2.Call<T>, response: retrofit2.Response<T>) {
            when {
                response.isSuccessful -> promise.complete(response)
                else                  -> {
                    val ex = retrofit2.HttpException(response)
                    if (call.isCanceled) {
                        cancelHandler(ex)
                        promise.cancel(true)
                    } else {
                        promise.completeExceptionally(ex)
                    }
                }
            }
        }

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
 * @param scheduler [Retry] 재시도 스케줄러
 * @param cancelHandler 취소 핸들러
 * @receiver
 * @return 비동기 호출 결과
 */
inline fun <T> retrofit2.Call<T>.executeAsync(
    retry: Retry,
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    return Decorators
        .ofCompletionStage { executeAsync(cancelHandler) }
        .withRetry(retry, scheduler)
        .get()
        .toCompletableFuture()
        .whenComplete { _, _ ->
            scheduler.shutdown()
        }
}
