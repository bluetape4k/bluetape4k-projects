package io.bluetape4k.concurrent

import io.bluetape4k.utils.ShutdownQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
 * Wraps a [Future] as a [CompletableFuture].
 *
 * The wrapper waits for [Future.get] on a shared virtual-thread executor so callers do not
 * allocate a new thread builder and unnamed watcher for every conversion. [cancel] still
 * propagates to the wrapped [Future] and cancels the watcher task.
 */
private class FutureToCompletableFutureWrapper<T>(private val wrapped: Future<T>): CompletableFuture<T>() {
    private val watcher: Future<*> = FutureWrapperExecutor.submit {
        try {
            complete(wrapped.get())
        } catch (e: CancellationException) {
            super.cancel(true)
        } catch (e: ExecutionException) {
            completeExceptionally(e.cause ?: e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            completeExceptionally(e)
        }
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        wrapped.cancel(mayInterruptIfRunning)
        watcher.cancel(true)
        return super.cancel(mayInterruptIfRunning)
    }
}

private object FutureWrapperExecutor {
    private val executor: ExecutorService = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("future-wrapper-", 0).factory()
    )

    init {
        ShutdownQueue.register(executor)
    }

    fun submit(task: Runnable): Future<*> =
        executor.submit(task)
}
