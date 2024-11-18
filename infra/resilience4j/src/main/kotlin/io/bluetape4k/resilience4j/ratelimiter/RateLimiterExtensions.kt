package io.bluetape4k.resilience4j.ratelimiter

import io.github.resilience4j.core.functions.CheckedRunnable
import io.github.resilience4j.ratelimiter.RateLimiter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer

/**
 * [runnable]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val runnable: Runnable = rateLimiter.runnable {
 *    // 실행할 코드
 *    println("Hello, World!")
 * }
 * runnable.run()
 * ```
 *
 * @param runnable 실행할 코드
 * @return RateLimiter가 적용된 Runnable
 */
inline fun RateLimiter.runnable(
    crossinline runnable: () -> Unit,
): () -> Unit = {
    RateLimiter.decorateRunnable(this) { runnable() }.run()
}

/**
 * [runnable]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val runnable: CheckedRunnable = rateLimiter.checkedRunnable {
 *    // 실행할 코드
 *    println("Hello, World!")
 * }
 * runnable.run()
 * ```
 *
 * @param runnable 실행할 코드
 * @return RateLimiter가 적용된 CheckedRunnable
 */
inline fun RateLimiter.checkedRunnable(
    crossinline runnable: () -> Unit,
): CheckedRunnable =
    RateLimiter.decorateCheckedRunnable(this) { runnable() }

/**
 * [callable]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val callable: Callable<String> = rateLimiter.callable {
 *   // 실행할 코드
 *   "Hello, World!"
 * }
 * val result: String = callable.call()  // "Hello, World!"
 * ```
 *
 * @param callable 실행할 코드
 * @return RateLimiter가 적용된 Callable
 */
inline fun <T> RateLimiter.callable(
    crossinline callable: () -> T,
): () -> T = {
    RateLimiter.decorateCallable(this) { callable() }.call()
}

/**
 * [supplier]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val supplier: Supplier<String> = rateLimiter.supplier {
 *   // 실행할 코드
 *   "Hello, World!"
 * }
 * val result: String = supplier.get()  // "Hello, World!"
 * ```
 *
 * @param supplier 실행할 코드
 * @return RateLimiter가 적용된 Supplier
 */
inline fun <T> RateLimiter.supplier(
    crossinline supplier: () -> T,
): () -> T = {
    RateLimiter.decorateSupplier(this) { supplier() }.get()
}

/**
 * [supplier]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val supplier: CheckedSupplier<String> = rateLimiter.checkedSupplier {
 *   // 실행할 코드
 *   "Hello, World!"
 * }
 * val result: String = supplier.get()  // "Hello, World!"
 * ```
 *
 * @param supplier 실행할 코드
 * @return RateLimiter가 적용된 CheckedSupplier
 */
inline fun <T> RateLimiter.checkedSupplier(
    crossinline supplier: () -> T,
): () -> T = {
    RateLimiter.decorateCheckedSupplier(this) { supplier() }.get()
}

/**
 * [consumer]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val consumer: Consumer<String> = rateLimiter.consumer {
 *   // 실행할 코드
 *   println(it)
 * }
 * consumer.accept("Hello, World!")  // "Hello, World!"
 * ```
 *
 * @param consumer 실행할 코드
 * @return RateLimiter가 적용된 Consumer
 */
inline fun <T> RateLimiter.consumer(
    crossinline consumer: (T) -> Unit,
): (T) -> Unit = { input: T ->
    RateLimiter.decorateConsumer(this, Consumer<T> { consumer(it) }).accept(input)
}

/**
 * [consumer]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val func = rateLimiter.function { input ->
 *      // 실행할 코드
 *      println(input)
 * }
 * func("Hello, World!")  // "Hello, World!"
 * ```
 *
 * @param consumer 실행할 코드
 * @return RateLimiter가 적용된 CheckedConsumer
 */
inline fun <T, R> RateLimiter.function(
    crossinline func: (T) -> R,
): (T) -> R = { input: T ->
    RateLimiter.decorateFunction<T, R>(this) { func(it) }.apply(input)
}

/**
 * [consumer]에 RateLimiter를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val func = rateLimiter.checkedFunction { input ->
 *      // 실행할 코드
 *      println(input)
 * }
 * func("Hello, World!")  // "Hello, World!"
 * ```
 *
 * @param consumer 실행할 코드
 * @return RateLimiter가 적용된 CheckedFunction
 */
inline fun <T, R> RateLimiter.checkedFunction(
    crossinline func: (T) -> R,
): (T) -> R = { input: T ->
    RateLimiter.decorateCheckedFunction<T, R>(this) { func(it) }.apply(input)
}

//
// 비동기 방식
//

/**
 * 비동기 함수인 [supplier]에 [RateLimiter]를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val supplier: Supplier<CompletableFuture<String>> = rateLimiter.completionStage {
 *           // 실행할 코드
 *           futureOf { 42 }
 * }
 * val result = supplier().get()  // 42
 * ```
 *
 * @param supplier 실행할 비동기 함수
 * @return RateLimiter가 적용된 Supplier
 */
inline fun <T> RateLimiter.completionStage(
    crossinline supplier: () -> CompletionStage<T>,
): () -> CompletionStage<T> = {
    RateLimiter.decorateCompletionStage(this) { supplier() }.get()
}

/**
 * 비동기 함수인 [supplier]에 [RateLimiter]를 적용합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val supplier: Supplier<CompletableFuture<String>> = rateLimiter.completableFuture { input ->
 *           // 실행할 코드
 *           futureOf { input * 2 }
 * }
 * val result = supplier(21).get()  // 42
 * ```
 *
 * @param supplier 실행할 비동기 함수
 * @return RateLimiter가 적용된 Supplier
 */
inline fun <T, R> RateLimiter.completableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> =
    decorateCompletableFuture(func)

/**
 * 비동기 함수인 [supplier]에 [RateLimiter]를 적용한 래핑 함수를 반환합니다.
 *
 * ```
 * val rateLimiter: RateLimiter = RateLimiter.ofDefaults("my")
 * val func = rateLimiter.decorateCompletableFuture { input ->
 *          // 실행할 코드
 *          futureOf { input * 2 }
 * }
 * val result = func(21).get()  // 42
 * ```
 *
 * @param func 실행할 비동기 함수
 * @return RateLimiter가 적용된 Supplier
 */
inline fun <T, R> RateLimiter.decorateCompletableFuture(
    crossinline func: (T) -> CompletableFuture<R>,
): (T) -> CompletableFuture<R> = { input: T ->
    val promise = CompletableFuture<R>()

    try {
        RateLimiter.waitForPermission(this)
        func(input)
            .whenComplete { result, error ->
                when (error) {
                    null -> promise.complete(result)
                    else -> promise.completeExceptionally(error)
                }
            }
    } catch (e: Throwable) {
        promise.completeExceptionally(e)
    }

    promise
}
