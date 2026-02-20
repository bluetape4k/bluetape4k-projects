package io.bluetape4k.resilience4j.retry

import io.github.resilience4j.core.functions.CheckedRunnable
import io.github.resilience4j.retry.Retry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * [runnable] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val runnable = retry.runnable {
 *    // 실행할 작업
 *    println("Hello, World!")
 *    // 예외 발생 시, 재시도
 * }
 * runnable.run()  // Hello, World!
 * ```
 *
 * @param runnable 실행할 작업
 * @return Retry 를 적용한 Runnable
 */
inline fun Retry.runnable(
    crossinline runnable: () -> Unit,
): Runnable {
    return Retry.decorateRunnable(this) { runnable() }
}

/**
 * [runnable] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val runnable = retry.checkedRunnable {
 *    // 실행할 작업
 *    println("Hello, World!")
 *    // 예외 발생 시, 재시도
 * }
 * runnable.run()  // Hello, World!
 * ```
 *
 * @param runnable 실행할 작업
 * @return Retry 를 적용한 Runnable
 */
inline fun Retry.checkedRunnable(
    crossinline runnable: () -> Unit,
): CheckedRunnable {
    return Retry.decorateCheckedRunnable(this) { runnable() }
}

/**
 * [callable] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val callable = retry.callable {
 *    // 실행할 작업
 *    println("Hello, World!")
 *    // 결과 반환
 *    42
 * }
 * val result = callable()  // Hello, World!
 * ```
 *
 * @param callable 실행할 작업
 * @return Retry 를 적용한 Callable
 */
inline fun <T> Retry.callable(
    crossinline callable: () -> T,
): () -> T = {
    Retry.decorateCallable(this) { callable() }.call()
}

/**
 * [supplier] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val supplier = retry.supplier {
 *    // 실행할 작업
 *    println("Hello, World!")
 *    // 결과 반환
 *    42
 * }
 * val result = supplier()  // Hello, World!
 * ```
 *
 * @param supplier 실행할 작업
 * @return Retry 를 적용한 Supplier
 */
inline fun <T> Retry.supplier(
    crossinline supplier: () -> T,
): () -> T = {
    Retry.decorateSupplier(this) { supplier() }.get()
}

/**
 * [supplier] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val supplier = retry.checkedSupplier {
 *    // 실행할 작업
 *    println("Hello, World!")
 *    // 결과 반환
 *    42
 * }
 * val result = supplier()  // Hello, World!
 * ```
 *
 * @param supplier 실행할 작업
 * @return Retry 를 적용한 Supplier
 */
inline fun <T> Retry.checkedSupplier(
    crossinline supplier: () -> T,
): () -> T = {
    Retry.decorateCheckedSupplier(this) { supplier() }.get()
}

/**
 * [func] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func = retry.function { input ->
 *    // 실행할 작업
 *    input * 2
 * }
 * val result = func(21)  // 42
 * ```
 *
 * @param func 실행할 작업
 * @return Retry 를 적용한 Function
 */
inline fun <T, R> Retry.function(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    Retry.decorateFunction<T, R>(this) { func(it) }.apply(input)
}

/**
 * [func] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func = retry.checkedFunction { input ->
 *    // 실행할 작업
 *    input * 2
 * }
 * val result = func(21)  // 42
 * ```
 *
 * @param func 실행할 작업
 * @return Retry 를 적용한 Function
 */
inline fun <T, R> Retry.checkedFunction(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    Retry.decorateCheckedFunction<T, R>(this) { func(it) }.apply(input)
}

//
// 비동기 방식
//

/**
 * 비동기 함수 [supplier] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func = withRetry(retry) { input -> futureOf { input * 2 } }
 * val result = func(21).get()  // 42
 * ```
 *
 * @param retry [Retry] 인스턴스
 * @param scheduler [ScheduledExecutorService] 인스턴스
 * @param supplier 실행할 비동기 Supplier
 * @return Retry 를 적용한 비동기 Supplier
 */
inline fun <T, R> withRetry(
    retry: Retry,
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline supplier: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> = { input: T ->
    Retry.decorateCompletionStage(retry, scheduler) { supplier.invoke(input) }
        .get()
        .toCompletableFuture()
        .whenComplete { _, _ ->
            runCatching {
                scheduler.shutdown()
                scheduler.awaitTermination(3, TimeUnit.SECONDS)
            }
        }
}


/**
 * 비동기 함수 [supplier] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val supplier = retry.completionStage {
 *    // 실행할 작업
 *    futureOf { 42 }
 * }
 * val result = supplier().get()  // 42
 * ```
 *
 * @param supplier 실행할 작업
 * @return Retry 를 적용한 Supplier
 */
inline fun <T> Retry.completionStage(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline supplier: () -> CompletionStage<T>,
): () -> CompletionStage<T> = {
    Retry.decorateCompletionStage(this, scheduler) { supplier() }
        .get()
        .whenComplete { _, _ ->
            runCatching {
                scheduler.shutdown()
                scheduler.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
}


/**
 * 비동기 함수 [func] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func = retry.completableFutureFunction { input -> futureOf { input * 2 } }
 * val result = func(21).get()  // 42
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 인스턴스
 * @param func 실행할 비동기 작업
 * @return Retry 를 적용한 비동기 Function
 */
inline fun <T, R> Retry.completableFutureFunction(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> {
    return completableFuture(scheduler, func)
}

/**
 * 비동기 함수 [func] 실행 시, [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func = retry.completableFuture { input -> futureOf { input * 2 } }
 * val result = func(21).get()  // 42
 * ```
 *
 * @param scheduler [ScheduledExecutorService] 인스턴스
 * @param func 실행할 비동기 작업
 * @return Retry 를 적용한 비동기 Function
 */
inline fun <T, R> Retry.completableFuture(
    scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> = { input: T ->
    this.executeCompletionStage<R>(scheduler) { func.invoke(input) }
        .toCompletableFuture()
        .whenComplete { _, _ ->
            runCatching {
                scheduler.shutdown()
                scheduler.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
}
