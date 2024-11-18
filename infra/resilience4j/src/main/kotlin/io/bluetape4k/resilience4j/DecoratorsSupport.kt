package io.bluetape4k.resilience4j

import io.bluetape4k.resilience4j.bulkhead.completableFuture
import io.bluetape4k.resilience4j.circuitbreaker.completableFuture
import io.bluetape4k.resilience4j.ratelimiter.completableFuture
import io.bluetape4k.resilience4j.retry.completableFuture
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * `Function1<T, CompletableFuture<R>>` 수형의 함수에 대해 Decoration을 제공합니다.
 *
 * ```kotlin
 *
 * val func: (String)->CompletableFuture<String> = { name: String ->futureOf { helloWorldService.returnHelloWorldWithName(name) } }
 *
 * val decorated (String)->CompletableFuture<String> = decorateCompletableFutureFunction(func)
 *                  .withRetry(Retry.ofDefaults("defaults"), scheduler)
 *                  .withCircuitBreaker(CircuitBreaker.ofDefaults("deafults"))
 *                  .withBulkhead(Bulkhead.ofDefaults("default"))
 *                  .withRateLimiter(RateLimiter.ofDefaults("default"))
 *                  .decorate()
 *
 *  val future:CompletableFuture<String> = decorated.invoke("world")
 * ```
 *
 * @param func function to decorate with resilience4j
 * @return DecorateCompletableFutureFunction<T, R>
 */
fun <T, R> decorateCompletableFutureFunction(
    func: (T) -> CompletableFuture<R>,
): DecorateCompletableFutureFunction<T, R> {
    return DecorateCompletableFutureFunction(func)
}

/**
 * `Funciton1<T, CompletableFuture<R>>` 함수를 bulkhead, circuit breaker, rate limiter, retry, time limiter
 * 를 적용할 수 있도록 decorate 합니다.
 *
 * ```kotlin
 *
 * val func: (String)->CompletableFuture<String> = { name: String ->futureOf { helloWorldService.returnHelloWorldWithName(name) } }
 *
 * val decorated (String)->CompletableFuture<String> = decorateCompletableFutureFunction(func)
 *                  .withRetry(Retry.ofDefaults("defaults"), scheduler)
 *                  .withCircuitBreaker(CircuitBreaker.ofDefaults("deafults"))
 *                  .withBulkhead(Bulkhead.ofDefaults("default"))
 *                  .withRateLimiter(RateLimiter.ofDefaults("default"))
 *                  .decorate()
 *
 *  val future:CompletableFuture<String> = decorated.invoke("world")
 * ```
 *
 * @param T input type
 * @param R return type
 * @property func function to decorate with resilience4j components
 */
class DecorateCompletableFutureFunction<T, R>(private var func: (T) -> CompletableFuture<R>) {

    fun withBulkhead(bulkhead: Bulkhead): DecorateCompletableFutureFunction<T, R> = apply {
        func = bulkhead.completableFuture(func)
    }

    fun withCircuitBreaker(circuitBreaker: CircuitBreaker): DecorateCompletableFutureFunction<T, R> = apply {
        func = circuitBreaker.completableFuture(func)
    }

    fun withRateLimiter(rateLimiter: RateLimiter): DecorateCompletableFutureFunction<T, R> = apply {
        func = rateLimiter.completableFuture(func)
    }

    fun withRetry(
        retry: Retry,
        scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    ): DecorateCompletableFutureFunction<T, R> = apply {
        func = retry.completableFuture(scheduler, func)
    }

    /**
     * [func] 함수를 decorate 합니다.
     *
     * @return
     */
    fun decorate(): (T) -> CompletableFuture<R> = { input: T -> func(input) }

    /**
     * decorated [func]을 실행합니다.
     *
     * @param input
     * @return
     */
    fun invoke(input: T): CompletableFuture<R> = func(input)
}
