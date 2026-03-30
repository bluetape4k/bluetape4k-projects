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
 * @param func resilience4j로 데코레이트할 함수
 * @return DecorateCompletableFutureFunction<T, R>
 */
fun <T, R> decorateCompletableFutureFunction(
    func: (T) -> CompletableFuture<R>,
): DecorateCompletableFutureFunction<T, R> = DecorateCompletableFutureFunction(func)

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
 * @param T 입력 타입
 * @param R 반환 타입
 * @property func resilience4j 컴포넌트로 데코레이트할 함수
 */
class DecorateCompletableFutureFunction<T, R>(
    private var func: (T) -> CompletableFuture<R>,
) {
    /**
     * [Bulkhead]를 적용합니다.
     *
     * @param bulkhead 적용할 bulkhead 인스턴스
     * @return 현재 데코레이터 빌더
     */
    fun withBulkhead(bulkhead: Bulkhead): DecorateCompletableFutureFunction<T, R> =
        apply {
            func = bulkhead.completableFuture(func)
        }

    /**
     * [CircuitBreaker]를 적용합니다.
     *
     * @param circuitBreaker 적용할 circuit breaker 인스턴스
     * @return 현재 데코레이터 빌더
     */
    fun withCircuitBreaker(circuitBreaker: CircuitBreaker): DecorateCompletableFutureFunction<T, R> =
        apply {
            func = circuitBreaker.completableFuture(func)
        }

    /**
     * [RateLimiter]를 적용합니다.
     *
     * @param rateLimiter 적용할 rate limiter 인스턴스
     * @return 현재 데코레이터 빌더
     */
    fun withRateLimiter(rateLimiter: RateLimiter): DecorateCompletableFutureFunction<T, R> =
        apply {
            func = rateLimiter.completableFuture(func)
        }

    /**
     * [Retry]를 적용합니다.
     *
     * @param retry 적용할 retry 인스턴스
     * @param scheduler 재시도 스케줄링에 사용할 executor
     * @return 현재 데코레이터 빌더
     */
    fun withRetry(
        retry: Retry,
        scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    ): DecorateCompletableFutureFunction<T, R> =
        apply {
            func = retry.completableFuture(scheduler, func)
        }

    /**
     * [func] 함수를 decorate 합니다.
     *
     * @return 현재까지 적용된 resilience4j 체인을 포함하는 함수
     */
    fun decorate(): (T) -> CompletableFuture<R> = { input: T -> func(input) }

    /**
     * decorated [func]을 실행합니다.
     *
     * @param input 입력 값
     * @return 실행 결과를 담은 [CompletableFuture]
     */
    fun invoke(input: T): CompletableFuture<R> = func(input)
}
