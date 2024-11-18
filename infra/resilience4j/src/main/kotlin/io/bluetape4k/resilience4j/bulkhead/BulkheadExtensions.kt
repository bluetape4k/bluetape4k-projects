package io.bluetape4k.resilience4j.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.core.functions.CheckedConsumer
import io.github.resilience4j.core.functions.CheckedRunnable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer

/**
 * Resilience4j의 Bulkhead 를 이용하여, [runnable] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val runnable = bulkhead.runnable {
 *  // 실행할 코드
 *  ...
 * }
 * runnable()
 * ```
 *
 * @param runnable 실행할 코드
 */
inline fun Bulkhead.runnable(
    crossinline runnable: () -> Unit,
): () -> Unit = {
    Bulkhead.decorateRunnable(this) { runnable() }.run()
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [runnable] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val runnable = bulkhead.checkedRunnable {
 *  // 실행할 코드
 *  ...
 * }
 * runnable()
 * ```
 *
 * @param runnable 실행할 코드
 */
inline fun Bulkhead.checkedRunnable(
    crossinline runnable: () -> Unit,
): CheckedRunnable {
    return Bulkhead.decorateCheckedRunnable(this) { runnable() }
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [callable] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val callable = bulkhead.callable {
 *  // 실행할 코드
 *  ...
 * }
 * val result = callable()
 * ```
 *
 * @param callable 실행할 코드
 */
inline fun <T> Bulkhead.callable(
    crossinline callable: () -> T,
): () -> T = {
    Bulkhead.decorateCallable(this) { callable() }.call()
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [supplier] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val supplier = bulkhead.supplier {
 *  // 실행할 코드
 *  ...
 * }
 * val result = supplier()
 * ```
 *
 * @param supplier 실행할 코드
 */
inline fun <T> Bulkhead.supplier(
    crossinline supplier: () -> T,
): () -> T = {
    Bulkhead.decorateSupplier(this) { supplier() }.get()
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [supplier] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val supplier = bulkhead.checkedSupplier {
 *  // 실행할 코드
 *  ...
 * }
 * val result = supplier()
 * ```
 *
 * @param supplier 실행할 코드
 */
inline fun <T> Bulkhead.checkedSupplier(
    crossinline supplier: () -> T,
): () -> T = {
    Bulkhead.decorateCheckedSupplier(this) { supplier() }.get()
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [consumer] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val consumer = bulkhead.consumer { input ->
 *  // 실행할 코드
 *  ...
 * }
 * consumer(input)
 * ```
 *
 * @param consumer 실행할 코드
 */
inline fun <T> Bulkhead.consumer(
    crossinline consumer: (T) -> Unit,
): (T) -> Unit = { input: T ->
    Bulkhead.decorateConsumer(this, Consumer<T> { consumer(it) }).accept(input)
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [consumer] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val consumer = bulkhead.checkedConsumer { input ->
 *  // 실행할 코드
 *  ...
 * }
 * consumer(input)
 * ```
 *
 * @param consumer 실행할 코드
 */
inline fun <T> Bulkhead.checkedConsumer(
    crossinline consumer: (T) -> Unit,
): CheckedConsumer<T> =
    Bulkhead.decorateCheckedConsumer(this) { consumer(it) }

/**
 * Resilience4j의 Bulkhead 를 이용하여, [func] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val func = bulkhead.function { input ->
 *      // 실행할 코드
 *      ...
 *      result
 * }
 * val result = func(input)
 * ```
 *
 * @param func 실행할 코드
 */
inline fun <T, R> Bulkhead.function(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    Bulkhead.decorateFunction<T, R>(this) { func(it) }.apply(input)
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [func] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val func = bulkhead.checkedFunction { input ->
 *      // 실행할 코드
 *      ...
 *      result
 * }
 * val result = func(input)
 * ```
 *
 * @param func 실행할 코드
 */
inline fun <T, R> Bulkhead.checkedFunction(
    crossinline func: (T) -> R,
): (T) -> R = { input ->
    Bulkhead.decorateCheckedFunction<T, R>(this) { func(it) }.apply(input)
}

//
// 비동기 방식 함수
//

/**
 * Resilience4j의 Bulkhead 를 이용하여, [supplier] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val supplier = bulkhead.completionStage {
 *  // 실행할 코드
 *  ...
 * }
 * val result = supplier().toCompletableFuture().get()
 * ```
 *
 * @param supplier 실행할 코드
 */
inline fun <T> Bulkhead.completionStage(
    crossinline supplier: () -> CompletionStage<T>,
): () -> CompletionStage<T> = {
    Bulkhead.decorateCompletionStage(this) { supplier() }.get()
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [func] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val func = bulkhead.completionStage { input ->
 *  // 실행할 코드
 *  ...
 * }
 * val result = func(input).toCompletableFuture().get()
 * ```
 *
 * @param func 실행할 코드
 */
inline fun <T, R> Bulkhead.completableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> {
    return decorateCompletableFuture(func)
}

/**
 * Resilience4j의 Bulkhead 를 이용하여, [func] 실행을 제어합니다.
 *
 * ```
 * val bulkhead = Bulkhead.ofDefaults("my-bulkhead")
 * val func = bulkhead.decorateCompletableFuture { input ->
 *  // 실행할 코드
 *  ...
 *  futureOf { 42 }
 * }
 * val result = func(input).get()
 * ```
 *
 * @param func 실행할 코드
 */
inline fun <T, R> Bulkhead.decorateCompletableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> = { input: T ->

    val promise = CompletableFuture<R>()

    if (!tryAcquirePermission()) {
        promise.completeExceptionally(BulkheadFullException.createBulkheadFullException(this))
    } else {
        try {
            func(input)
                .whenComplete { result, error ->
                    onComplete()
                    when (error) {
                        null -> promise.complete(result)
                        else -> promise.completeExceptionally(error)
                    }
                }
        } catch (throwable: Throwable) {
            onComplete()
            promise.completeExceptionally(throwable)
        }
    }

    promise
}
