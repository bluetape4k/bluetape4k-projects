package io.bluetape4k.retrofit2

import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val retryScheduler by lazy {
    Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r).apply { isDaemon = true }
    }
}

/**
 * Retrofit [retrofit2.Call]을 비동기로 실행해 [CompletableFuture]를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `enqueue` 기반으로 future를 완료합니다.
 * - 호출 취소 상태에서 콜백이 도착하면 [cancelHandler]를 호출하고 future를 `cancel(true)` 처리합니다.
 * - 네트워크 실패는 `completeExceptionally`로 전달됩니다.
 *
 * ```kotlin
 * val response = api.getPost(1).executeAsync().get()
 * // response.isSuccessful == true
 * ```
 *
 * @param cancelHandler 취소 경로에서 호출할 콜백입니다.
 */
inline fun <T> retrofit2.Call<T>.executeAsync(
    crossinline cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    val promise = CompletableFuture<retrofit2.Response<T>>()

    val callback = object: retrofit2.Callback<T> {
        override fun onResponse(call: retrofit2.Call<T>, response: retrofit2.Response<T>) {
            if (call.isCanceled) {
                val ex = retrofit2.HttpException(response)
                cancelHandler(ex)
                promise.cancel(true)
            } else {
                promise.complete(response)
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
 * Retrofit [retrofit2.Call] 비동기 실행에 Resilience4j [Retry]를 적용합니다.
 *
 * ## 동작/계약
 * - 기본 실행은 [executeAsync]를 사용합니다.
 * - 재시도마다 현재 [retrofit2.Call]을 `clone()`한 새 인스턴스로 실행합니다.
 * - 재시도는 단일 스레드 스케줄러에서 처리됩니다.
 * - [retry] 설정에 따라 실패 future가 재호출될 수 있습니다.
 *
 * ```kotlin
 * val response = api.getPost(1).executeAsync(retry).get()
 * // response.isSuccessful == true
 * ```
 *
 * @param retry 적용할 재시도 정책입니다.
 * @param cancelHandler 취소 경로에서 호출할 콜백입니다.
 */
fun <T> retrofit2.Call<T>.executeAsync(
    retry: Retry,
    cancelHandler: (Throwable?) -> Unit = {},
): CompletableFuture<retrofit2.Response<T>> {
    return Decorators
        .ofCompletionStage { clone().executeAsync(cancelHandler) }
        .withRetry(retry, retryScheduler)
        .get()
        .toCompletableFuture()
}
