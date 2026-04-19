package io.bluetape4k.concurrent

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * [Future] 인스턴스를 [CompletionStage] 로 변환합니다.
 *
 * ```kotlin
 * val future: Future<Long> = Executors.newSingleThreadExecutor().submit { 42L }
 * val stage: CompletionStage<Long> = future.asCompletionStage()
 * ```
 *
 * @return [CompletionStage] 인스턴스
 */
@Suppress("UNCHECKED_CAST")
fun <T> Future<T>.asCompletionStage(): CompletionStage<T> = when (this@asCompletionStage) {
    is CompletionStage<*> -> this@asCompletionStage as CompletionStage<T>
    else -> FutureToCompletableFutureWrapper(this)
}

/**
 * [Future] 인스턴스를 [CompletableFuture] 로 변환합니다.
 *
 * ```kotlin
 * val future: Future<Long> = Executors.newSingleThreadExecutor().submit { 42L }
 * val completableFuture: CompletableFuture<Long> = future.asCompletableFuture()
 * ```
 *
 * @return [CompletableFuture] 인스턴스
 */
fun <T> Future<T>.asCompletableFuture(): CompletableFuture<T> = when (this@asCompletableFuture) {
    is CompletableFuture<*> -> this@asCompletableFuture as CompletableFuture<T>
    else -> FutureToCompletableFutureWrapper(this)
}

/**
 * [Future]를 [CompletableFuture]로 변환하는 래퍼.
 * Virtual thread에서 [Future.get]으로 블로킹 대기하여 폴링 오버헤드를 제거합니다.
 */
private class FutureToCompletableFutureWrapper<T>(future: Future<T>): CompletableFuture<T>() {
    init {
        Thread.ofVirtual().name("future-wrapper").start {
            try {
                complete(future.get())
            } catch (e: CancellationException) {
                cancel(true)
            } catch (e: ExecutionException) {
                completeExceptionally(e.cause ?: e)
            } catch (e: InterruptedException) {
                completeExceptionally(e)
            }
        }
    }
}
