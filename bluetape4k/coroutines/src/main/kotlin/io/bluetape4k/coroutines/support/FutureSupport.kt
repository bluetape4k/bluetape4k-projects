package io.bluetape4k.coroutines.support

import io.bluetape4k.concurrent.asCompletableFuture
import io.bluetape4k.concurrent.sequence
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.seconds

/**
 * `Future`를 중단 가능 방식으로 대기하고 완료 값을 반환합니다.
 *
 * ## Behaviour / Contract
 * - Delegates to `await()` directly when the receiver is a `CompletionStage`.
 * - Wraps other `Future` types via `asCompletableFuture()` then awaits.
 * - Throws `CancellationException` if the `Future` is already cancelled.
 * - Cancelling the caller propagates to the underlying `Future` via `cancel(false)`.
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
suspend fun <T> Future<T>.suspendAwait(): T = awaitSuspending()

/**
 * Future 비동기 호출을 bounded coroutine suspension으로 소비한다.
 *
 * ### Note
 * `runTest` 는 가상의 시간을 사용하므로, withTimeout 이 제대로 동작하지 않는다.
 *
 * Timeout 또는 호출자 취소가 발생하면 아직 완료되지 않은 Future에
 * client-side [java.util.concurrent.Future.cancel]을 best-effort로 시도해
 * 테스트 종료 뒤의 pending wait를 줄인다.
 * 원격 실행 취소까지 보장하는 helper는 아니며, 원래의 cancellation 원인은 그대로 다시 던진다.
 */
suspend fun <T> Future<T>.awaitUntil(timeout: kotlin.time.Duration = 5.seconds): T {
    return try {
        withTimeout(timeout) { this@awaitUntil.awaitSuspending() }
    } catch (cause: TimeoutCancellationException) {
        cancel(false)
        throw cause
    } catch (cause: kotlinx.coroutines.CancellationException) {
        cancel(false)
        throw cause
    }
}

/**
 * Future 비동기 호출을 bounded coroutine suspension으로 소비한다.
 *
 * ### Note
 * `runTest` 는 가상의 시간을 사용하므로, withTimeout 이 제대로 동작하지 않는다.
 *
 * Timeout 또는 호출자 취소가 발생하면 아직 완료되지 않은 Future에
 * client-side [java.util.concurrent.Future.cancel]을 best-effort로 시도해
 * 테스트 종료 뒤의 pending wait를 줄인다.
 * 원격 실행 취소까지 보장하는 helper는 아니며, 원래의 cancellation 원인은 그대로 다시 던진다.
 */
suspend fun <T> Iterable<Future<T>>.awaitAllUntil(timeout: kotlin.time.Duration = 5.seconds): List<T> =
    map { it.asCompletableFuture() }.sequence().awaitUntil(timeout)
