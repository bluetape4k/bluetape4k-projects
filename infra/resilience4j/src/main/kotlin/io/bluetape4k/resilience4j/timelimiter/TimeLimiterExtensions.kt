package io.bluetape4k.resilience4j.timelimiter

import io.bluetape4k.resilience4j.SchedulerHandle
import io.github.resilience4j.timelimiter.TimeLimiter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
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
 * @param scheduler 호출자가 소유한 스케줄러. `null`이면 호출마다 전용 스케줄러를 만들고 완료 후 종료하며,
 * 제공된 스케줄러는 종료하지 않으므로 호출자가 수명주기를 관리해야 합니다.
 * @param futureSupplier 실행할 CompletionStage 를 생성하는 함수
 */
inline fun <T, F: CompletionStage<T>> TimeLimiter.completionStage(
    scheduler: ScheduledExecutorService? = null,
    crossinline futureSupplier: () -> F,
): () -> T = {
    val handle = SchedulerHandle.acquire(scheduler)
    handle.execute { executor ->
        TimeLimiter
            .decorateCompletionStage(this, executor) { futureSupplier.invoke() }
            .get()
            .whenComplete { _, _ -> handle.close() }
            .toCompletableFuture()
            .get()
    }
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
 * @param scheduler 호출자가 소유한 스케줄러. `null`이면 호출마다 전용 스케줄러를 만들고 terminal completion 후 종료하며,
 * 제공된 스케줄러는 종료하지 않으므로 호출자가 수명주기를 관리해야 합니다.
 * @param func 실행할 CompletableFuture 를 생성하는 함수
 */
inline fun <T, R: CompletableFuture<T>> TimeLimiter.completableFuture(
    scheduler: ScheduledExecutorService? = null,
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
 * @param scheduler 호출자가 소유한 스케줄러. `null`이면 호출마다 전용 스케줄러를 만들고 terminal completion 후 종료하며,
 * 제공된 스케줄러는 종료하지 않으므로 호출자가 수명주기를 관리해야 합니다.
 * @param func 실행할 CompletableFuture 를 생성하는 함수
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R: CompletableFuture<T>> TimeLimiter.decorateCompletableFuture(
    scheduler: ScheduledExecutorService? = null,
    crossinline func: (T) -> R,
): (T) -> R = { input: T ->
    val handle = SchedulerHandle.acquire(scheduler)
    handle.execute { executor ->
        this.executeCompletionStage<T, R>(executor) { func(input) }
            .toCompletableFuture()
            .whenComplete { _, _ -> handle.close() } as R
    }
}
