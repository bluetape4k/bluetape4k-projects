package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.vertx.core.Future
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Resilience4j의 다양한 요소들을 Vertx 작업 수행 시 Decorator로 사용할 수 있도록 지원합니다.
 *
 * ```kotlin
 * val circuitBreaker = CircuitBreaker.ofDefaults("test")
 * val retry = Retry.ofDefaults("test")
 * val decorated = VertxDecorators.ofSupplier { Future.succeededFuture("ok") }
 *     .withRetry(retry)
 *     .withCircuitBreaker(circuitBreaker)
 *     .decorate()
 * val result = decorated().asCompletableFuture().get()
 * // result == "ok"
 * ```
 */
object VertxDecorators: KLoggingChannel() {

    /**
     * Vert.x [supplier]를 decorate 합니다.
     *
     * ```kotlin
     * val circuitBreaker = CircuitBreaker.ofDefaults("test")
     * val retry = Retry.ofDefaults("test")
     *
     * val decorated = VertxDecorators.ofSupplier { service.returnHelloWorld() }
     *     .withRetry(retry)
     *     .withCircuitBreaker(circuitBreaker)
     *     .decorate()
     *
     * val result = runCatching { decorated().asCompletableFuture().get() }
     * // result.getOrNull() == "Hello World"
     * ```
     *
     * @param supplier Resilience4j Component 들로 decorate 할 Vert.x [Future]를 생성하는 함수
     * @return [supplier] 를 decorate 한 [SuspendVertxDecorateSupplier]
     */
    fun <T> ofSupplier(supplier: () -> Future<T>): SuspendVertxDecorateSupplier<T> {
        return SuspendVertxDecorateSupplier(supplier)
    }

    /**
     * Vert.x [Future] supplier 에 여러 Resilience4j 컴포넌트를 체이닝하는 빌더 클래스입니다.
     *
     * ```kotlin
     * val supplier = SuspendVertxDecorateSupplier<String> { Future.succeededFuture("ok") }
     *     .withCircuitBreaker(CircuitBreaker.ofDefaults("cb"))
     *     .withRetry(Retry.ofDefaults("retry"))
     * val future = supplier.invoke()
     * // future.result() == "ok"
     * ```
     */
    class SuspendVertxDecorateSupplier<T>(private var supplier: () -> Future<T>) {

        /** [Bulkhead]를 적용합니다. */
        fun withBulkhead(bulkhead: Bulkhead) = apply {
            supplier = bulkhead.decorateVertxFuture(supplier)
        }

        /** [CircuitBreaker]를 적용합니다. */
        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            supplier = circuitBreaker.decorateVertxFuture(supplier)
        }

        /** [RateLimiter]를 적용합니다. */
        fun withRateLimiter(rateLimiter: RateLimiter) = apply {
            supplier = rateLimiter.decorateVertxFuture(supplier)
        }

        /** [Retry]를 적용합니다. */
        fun withRetry(
            retry: Retry,
            scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
        ) = apply {
            supplier = retry.decorateVertxFuture(scheduler, supplier)
        }

        /** [TimeLimiter]를 적용합니다. */
        fun withTimeLimiter(
            timeLimiter: TimeLimiter,
            scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
        ) = apply {
            supplier = timeLimiter.decorateVertxFuture(scheduler, supplier)
        }

        /** 성공/실패 모두 처리하는 폴백 핸들러를 적용합니다. */
        fun withFallback(
            handler: (T?, Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(handler)
        }

        /** 예외 발생 시 복구하는 폴백 핸들러를 적용합니다. */
        fun withFallback(
            exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionHandler)
        }

        /** 결과 조건에 따라 복구하는 폴백 핸들러를 적용합니다. */
        fun withFallback(
            resultPredicate: (T) -> Boolean,
            resultHandler: (T) -> T,
        ) = apply {
            supplier = supplier.recover(resultPredicate, resultHandler)
        }

        /** 특정 예외 타입 발생 시 복구하는 폴백 핸들러를 적용합니다. */
        fun withFallback(
            exceptionType: Class<out Throwable>,
            exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionType, exceptionHandler)
        }

        /** 여러 예외 타입 발생 시 복구하는 폴백 핸들러를 적용합니다. */
        fun withFallback(
            exceptionTypes: Iterable<Class<out Throwable>>,
            exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionTypes, exceptionHandler)
        }

        /** 현재까지 체이닝된 모든 decorator가 적용된 supplier 함수를 반환합니다. */
        fun decorate(): () -> Future<T> = supplier

        /** 체이닝된 supplier를 즉시 실행하고 [Future]를 반환합니다. */
        fun invoke(): Future<T> = decorate().invoke()
    }
}
