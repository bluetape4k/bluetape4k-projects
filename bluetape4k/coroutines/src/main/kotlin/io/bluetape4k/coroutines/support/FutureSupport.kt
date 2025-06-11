package io.bluetape4k.coroutines.support

import io.bluetape4k.concurrent.asCompletableFuture
import kotlinx.coroutines.future.await
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future

/**
 * [Future] 를 Coroutines 환경에서 non-blocking 으로 완료될 때까지 대기합니다.
 *
 * ```
 * val future = futureOf { Thread.sleep(1000); 1 }
 * val result = future.suspendAwait() // 1
 * ```
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> Future<T>.suspendAwait(): T = when (this) {
    is CompletionStage<*> -> await() as T
    else -> when {
        isCancelled -> throw CancellationException()
        else -> this.asCompletableFuture().await()
    }
}

/**
 * [Future] 를 Coroutines 환경에서 non-blocking 으로 완료될 때까지 대기합니다.
 *
 * ```
 * val future = futureOf { Thread.sleep(1000); 1 }
 * val result = future.coAwait() // 1
 * ```
 */
@Deprecated(
    message = "Use suspendAwait() instead.",
    replaceWith = ReplaceWith("suspendAwait()")
)
@Suppress("UNCHECKED_CAST")
suspend fun <T> Future<T>.coAwait(): T = when (this) {
    is CompletionStage<*> -> await() as T
    else -> when {
        isCancelled -> throw CancellationException()
        else -> this.asCompletableFuture().await()
    }
}
