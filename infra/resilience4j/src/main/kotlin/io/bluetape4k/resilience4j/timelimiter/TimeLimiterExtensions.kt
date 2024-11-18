package io.bluetape4k.resilience4j.timelimiter

import io.github.resilience4j.timelimiter.TimeLimiter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService

/**
 * [futureSupplier] 를 실행할 때 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val func = timeLimiter.futureSupplier {
 *   // 실행할 Future 생성
 *   futureOf { 42 }
 * }
 * val result = func()  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param futureSupplier 실행할 Future 를 생성하는 함수
 */
inline fun <T, F: Future<T>> TimeLimiter.futureSupplier(
    crossinline futureSupplier: () -> F,
): () -> T = {
    TimeLimiter.decorateFutureSupplier(this) { futureSupplier.invoke() }.call()
}

/**
 * [futureSupplier] 를 실행할 때 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val func = timeLimiter.completionStage {
 *   // 실행할 CompletionStage 생성
 *   completionStageOf { 42 }
 * }
 * val result = func()  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param scheduler 스케줄러
 * @param futureSupplier 실행할 CompletionStage 를 생성하는 함수
 */
inline fun <T, F: CompletionStage<T>> TimeLimiter.completionStage(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline futureSupplier: () -> F,
): () -> T = {
    TimeLimiter
        .decorateCompletionStage(this, scheduler) { futureSupplier.invoke() }
        .get()
        .whenComplete { _, _ -> scheduler.shutdown() }
        .toCompletableFuture()
        .get()
}

/**
 * [func] 를 실행할 때 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val func = timeLimiter.completableFuture { input ->
 *   // 실행할 CompletableFuture 생성
 *   futureOf { input * 2 }
 * }
 * val result = func(21).get()  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param scheduler 스케줄러
 * @param func 실행할 CompletableFuture 를 생성하는 함수
 */
inline fun <T, R: CompletableFuture<T>> TimeLimiter.completableFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline func: (T) -> R,
): (T) -> R {
    return decorateCompletableFuture(scheduler, func)
}

/**
 * [func] 를 실행할 때 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val func = timeLimiter.decorateCompletableFuture { input ->
 *   // 실행할 CompletableFuture 생성
 *   futureOf { input * 2 }
 * }
 * val result = func(21).get()  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param scheduler 스케줄러
 * @param func 실행할 CompletableFuture 를 생성하는 함수
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R: CompletableFuture<T>> TimeLimiter.decorateCompletableFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline func: (T) -> R,
): (T) -> R = { input: T ->
    this.executeCompletionStage<T, R>(scheduler) { func(input) }
        .toCompletableFuture()
        .whenComplete { _, _ -> scheduler.shutdown() }
            as R
}
