package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * [Future] 인스턴스를 [CompletionStage] 로 변환합니다.
 *
 * ```
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
 * ```
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

private class FutureToCompletableFutureWrapper<T> private constructor(
    private val future: Future<T>,
): CompletableFuture<T>() {

    companion object: KLogging() {
        @JvmStatic
        operator fun <T> invoke(future: Future<T>): FutureToCompletableFutureWrapper<T> =
            FutureToCompletableFutureWrapper(future).apply {
                schedule { tryToComplete() }
            }
    }

    private val service = Executors.newSingleThreadScheduledExecutor()

    private inline fun schedule(crossinline action: () -> Unit) {
        service.schedule({ action() }, 100, TimeUnit.NANOSECONDS)
    }

    private fun tryToComplete() {
        try {
            if (future.isDone) {
                try {
                    this.complete(future.get())
                } catch (e: InterruptedException) {
                    this.completeExceptionally(e.cause ?: e)
                } catch (e: ExecutionException) {
                    this.completeExceptionally(e.cause ?: e)
                }
                service.shutdown()
                return
            }
            if (future.isCancelled) {
                this.cancel(true)
                service.shutdown()
                return
            }
            schedule { tryToComplete() }
        } catch (e: Throwable) {
            log.error(e) { "Future 인스턴스가 예외를 발생시켰습니다." }
            this.completeExceptionally(e.cause ?: e)
            service.shutdown()
        }
    }
}
