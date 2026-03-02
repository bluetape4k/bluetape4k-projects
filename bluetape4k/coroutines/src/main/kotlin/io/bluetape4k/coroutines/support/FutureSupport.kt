package io.bluetape4k.coroutines.support

import io.bluetape4k.concurrent.asCompletableFuture
import kotlinx.coroutines.future.await
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future

/**
 * `Future`를 중단 가능 방식으로 대기하고 완료 값을 반환합니다.
 *
 * ## 동작/계약
 * - `CompletionStage` 구현체면 `await()`를 사용해 바로 대기합니다.
 * - 그 외 `Future`는 `asCompletableFuture()`로 변환해 `await()`로 대기합니다.
 * - 이미 취소된 `Future`면 `CancellationException`을 던집니다.
 *
 * ```kotlin
 * val future = java.util.concurrent.CompletableFuture.completedFuture(42)
 * val result = future.awaitSuspending()
 * // result == 42
 * ```
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> Future<T>.awaitSuspending(): T = when (this) {
    is CompletionStage<*> -> await() as T
    else                  -> when {
        isCancelled -> throw CancellationException()
        else        -> this.asCompletableFuture().await()
    }
}

/**
 * [awaitSuspending]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 내부 동작은 [awaitSuspending]과 동일합니다.
 * - 취소된 `Future`에 대해 `CancellationException`을 던집니다.
 * - 신규 코드는 `awaitSuspending()` 사용을 권장합니다.
 *
 * ```kotlin
 * val future = java.util.concurrent.CompletableFuture.completedFuture(42)
 * val result = future.suspendAwait()
 * // result == 42
 * ```
 */
@Deprecated("use awaitSuspending() instead.", replaceWith = ReplaceWith("awaitSuspending()"))
@Suppress("UNCHECKED_CAST")
suspend fun <T> Future<T>.suspendAwait(): T = when (this) {
    is CompletionStage<*> -> await() as T
    else -> when {
        isCancelled -> throw CancellationException()
        else -> this.asCompletableFuture().await()
    }
}
