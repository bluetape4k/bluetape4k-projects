package io.bluetape4k.resilience4j.circuitbreaker

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.core.functions.CheckedConsumer
import io.github.resilience4j.core.functions.CheckedRunnable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

/**
 * CircuitBreaker 를 적용하여 [runnable]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val runnableWithCB = circuitBreaker.runnable {
 *     // 실행할 코드
 * }
 * runnableWithCB()
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param runnable 실행할 코드
 * @return CircuitBreaker 를 적용한 Runnable
 */
inline fun CircuitBreaker.runnable(crossinline runnable: () -> Unit): () -> Unit = {
    CircuitBreaker.decorateRunnable(this) { runnable() }.run()
}

/**
 * CircuitBreaker 를 적용하여 [runnable]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val runnableWithCB = circuitBreaker.checkedRunnable {
 *     // 실행할 코드
 * }
 * runnableWithCB()
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param runnable 실행할 코드
 * @return CircuitBreaker 를 적용한 CheckedRunnable
 */
inline fun CircuitBreaker.checkedRunnable(crossinline runnable: () -> Unit): CheckedRunnable =
    CircuitBreaker.decorateCheckedRunnable(this) { runnable() }

/**
 * CircuitBreaker 를 적용하여 [callable]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val callableWithCB = circuitBreaker.callable {
 *    // 실행할 코드
 *    42
 * }
 * val result = callableWithCB()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param callable 실행할 코드
 * @return CircuitBreaker 를 적용한 Callable
 */
inline fun <T> CircuitBreaker.callable(
    crossinline callable: () -> T,
): () -> T = {
    CircuitBreaker.decorateCallable(this) { callable() }.call()
}

/**
 * CircuitBreaker 를 적용하여 [supplier]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val supplierWithCB = circuitBreaker.supplier {
 *    // 실행할 코드
 *    42
 * }
 * val result = supplierWithCB()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param supplier 실행할 코드
 * @return CircuitBreaker 를 적용한 Supplier
 */
inline fun <T> CircuitBreaker.supplier(
    crossinline supplier: () -> T,
): () -> T = {
    CircuitBreaker.decorateSupplier(this) { supplier() }.get()
}

/**
 * CircuitBreaker 를 적용하여 [supplier]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val supplierWithCB = circuitBreaker.checkedSupplier {
 *    // 실행할 코드
 *    42
 * }
 * val result = supplierWithCB()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param supplier 실행할 코드
 * @return CircuitBreaker 를 적용한 Checked Supplier
 */
inline fun <T> CircuitBreaker.checkedSupplier(
    crossinline supplier: () -> T,
): () -> T = {
    CircuitBreaker.decorateCheckedSupplier(this) { supplier() }.get()
}

/**
 * CircuitBreaker 를 적용하여 [consumer]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val consumer = circuitBreaker.consumer { input ->
 *    // 실행할 코드
 * }
 * consumer("input")
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param consumer 실행할 코드
 * @return CircuitBreaker 를 적용한 Consumer
 */
inline fun <T> CircuitBreaker.consumer(
    crossinline consumer: (T) -> Unit,
): (T) -> Unit = { input: T ->
    CircuitBreaker.decorateConsumer<T>(this) { consumer(it) }.accept(input)
}

/**
 * CircuitBreaker 를 적용하여 [consumer]을 실행합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val consumer = circuitBreaker.checkedConsumer { input ->
 *    // 실행할 코드
 * }
 * consumer("input")
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param consumer 실행할 코드
 * @return CircuitBreaker 를 적용한 Checked Consumer
 */
inline fun <T> CircuitBreaker.checkedConsumer(
    crossinline consumer: (T) -> Unit,
): CheckedConsumer<T> {
    return CircuitBreaker.decorateCheckedConsumer(this) { consumer(it) }
}

/**
 * CircuitBreaker 를 적용하여 [func]을 실행하도록 decorate 합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val func = circuitBreaker.function { input ->
 *    // 실행할 코드
 *    input * 2
 * }
 * val result = func(21)  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param func 실행할 코드
 * @return CircuitBreaker 를 적용한 Function
 */
inline fun <T, R> CircuitBreaker.function(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    CircuitBreaker.decorateFunction<T, R>(this) { func(it) }.apply(input)
}

/**
 * CircuitBreaker 를 적용하여 [func]을 실행하도록 decorate 합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val func = circuitBreaker.checkedFunction { input ->
 *    // 실행할 코드
 *    input * 2
 * }
 * val result = func(21)  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param func 실행할 코드
 * @return CircuitBreaker 를 적용한 Checked Function
 */
inline fun <T, R> CircuitBreaker.checkedFunction(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    CircuitBreaker.decorateCheckedFunction<T, R>(this) { func(it) }.apply(input)
}

//
// 비동기 방식
//

/**
 * 비동기 함수([supplier]]를 실행할 때, CircuitBreaker 를 적용합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val func = circuitBreaker.completionStatge {
 *    // 실행할 코드
 *    futureOf { 42 }
 * }
 * val result = func().get()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param supplier 실행할 코드
 * @return CircuitBreaker 를 적용한 Supplier
 */
inline fun <T> CircuitBreaker.completionStatge(
    crossinline supplier: () -> CompletionStage<T>,
): () -> CompletionStage<T> = {
    CircuitBreaker.decorateCompletionStage(this) { supplier() }.get()
}

/**
 * 비동기 함수([supplier]]를 실행할 때, CircuitBreaker 를 적용합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val func = circuitBreaker.completableFuture { input: Int ->
 *    // 실행할 코드
 *    futureOf { input * 2 }
 * }
 * val result = func(21).get()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param supplier 실행할 코드
 * @return CircuitBreaker 를 적용한 Supplier
 */
inline fun <T, R> CircuitBreaker.completableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> {
    return decorateCompletableFuture(func)
}

/**
 * 비동기 함수([supplier]]를 실행할 때, CircuitBreaker 를 적용합니다.
 *
 * ```
 * val circuitBreaker: CircuitBreaker = ...
 * val func = circuitBreaker.decorateCompletableFuture { input: Int ->
 *    // 실행할 코드
 *    futureOf { input * 2 }
 * }
 * val result = func(21).get()  // 42
 * ```
 *
 * @receiver CircuitBreaker 인스턴스
 * @param func 실행할 코드
 * @return CircuitBreaker 를 적용한 Function
 */
inline fun <T, R> CircuitBreaker.decorateCompletableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> = { input: T ->

    val promise = CompletableFuture<R>()

    if (!tryAcquirePermission()) {
        promise.completeExceptionally(CallNotPermittedException.createCallNotPermittedException(this))
    } else {
        val start = System.nanoTime()
        try {
            func(input)
                .whenComplete { result, error ->
                    val durationInNanos = System.nanoTime() - start
                    if (error != null) {
                        if (error is Exception) {
                            onError(durationInNanos, TimeUnit.NANOSECONDS, error)
                        }
                        promise.completeExceptionally(error)
                    } else {
                        onSuccess(durationInNanos, TimeUnit.NANOSECONDS)
                        promise.complete(result)
                    }
                }
        } catch (e: Throwable) {
            val durationInNanos = System.nanoTime() - start
            onError(durationInNanos, TimeUnit.NANOSECONDS, e)
            promise.completeExceptionally(e)
        }
    }

    promise
}
