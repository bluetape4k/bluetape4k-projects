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
 */
object VertxDecorators: KLoggingChannel() {

    /**
     * Vert.x [supplier]를 decorate 합니다.
     *
     * ```
     * val circuitBreaker = CircuitBreaker.ofDefaults("test")
     * val retry = Retry.ofDefaults("test")
     *
     * val decorated = VertxDecorators.ofSupplier { service.returnHelloWorld() }
     *     .withRetry(retry)
     *     .withCircuitBreaker(circuitBreaker)
     *     .decorate()
     *
     * val result = runCatching { decorated().asCompletableFuture().get() }
     * ```
     *
     * @param supplier Resilience4j Component 들로 decorate 할 Vert.x [Future]를 생성하는 함수
     * @param [supplier] 를 decorate 한 함수
     */
    fun <T> ofSupplier(@BuilderInference supplier: () -> Future<T>): SuspendVertxDecorateSupplier<T> {
        return SuspendVertxDecorateSupplier(supplier)
    }

    class SuspendVertxDecorateSupplier<T>(private var supplier: () -> Future<T>) {

        fun withBulkhead(bulkhead: Bulkhead) = apply {
            supplier = bulkhead.decorateVertxFuture(supplier)
        }

        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            supplier = circuitBreaker.decorateVertxFuture(supplier)
        }

        fun withRateLimiter(rateLimiter: RateLimiter) = apply {
            supplier = rateLimiter.decorateVertxFuture(supplier)
        }

        fun withRetry(
            retry: Retry,
            scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
        ) = apply {
            supplier = retry.decorateVertxFuture(scheduler, supplier)
        }

        fun withTimeLimiter(
            timeLimiter: TimeLimiter,
            scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
        ) = apply {
            supplier = timeLimiter.decorateVertxFuture(scheduler, supplier)
        }

        fun withFallback(
            @BuilderInference handler: (T?, Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(handler)
        }

        fun withFallback(
            @BuilderInference exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionHandler)
        }

        fun withFallback(
            resultPredicate: (T) -> Boolean,
            @BuilderInference resultHandler: (T) -> T,
        ) = apply {
            supplier = supplier.recover(resultPredicate, resultHandler)
        }

        fun withFallback(
            exceptionType: Class<out Throwable>,
            @BuilderInference exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionType, exceptionHandler)
        }

        fun withFallback(
            exceptionTypes: Iterable<Class<out Throwable>>,
            @BuilderInference exceptionHandler: (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionTypes, exceptionHandler)
        }

        fun decorate(): () -> Future<T> = supplier

        fun invoke(): Future<T> = decorate().invoke()
    }
}
