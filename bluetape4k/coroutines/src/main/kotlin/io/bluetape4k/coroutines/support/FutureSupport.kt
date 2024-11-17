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
 * val result = future.coAwait() // 1
 * ```
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> Future<T>.coAwait(): T = when (this) {
    is CompletionStage<*> ->
        await() as T

    else                  ->
        when {
// NOTE: isDone 이 되었으면 이 값을 사용할 수 있어야 하는데 ... 뭔가 무한루프에 빠져서 아예 빼 버렸다.
//            isDone      -> {
//                try {
//                    withContext(coroutineContext) { get() }
//                } catch (e: ExecutionException) {
//                    throw e.cause ?: e
//                }
//            }

            isCancelled -> throw CancellationException()
            else        -> this.asCompletableFuture().await()
        }
}
