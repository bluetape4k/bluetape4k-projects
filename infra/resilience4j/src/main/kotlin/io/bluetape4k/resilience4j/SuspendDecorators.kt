package io.bluetape4k.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.bulkhead.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.bulkhead.decorateSuspendFunction1
import io.bluetape4k.resilience4j.cache.SuspendCache
import io.bluetape4k.resilience4j.cache.decorateSuspendFunction
import io.bluetape4k.resilience4j.circuitbreaker.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.circuitbreaker.decorateSuspendFunction1
import io.bluetape4k.resilience4j.ratelimiter.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.ratelimiter.decorateSuspendFunction1
import io.bluetape4k.resilience4j.retry.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.retry.decorateSuspendFunction1
import io.bluetape4k.resilience4j.timelimiter.decorateSuspendBiFunction
import io.bluetape4k.resilience4j.timelimiter.decorateSuspendFunction1
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.cache.Cache
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.bulkhead.decorateSuspendFunction
import io.github.resilience4j.kotlin.circuitbreaker.decorateSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.decorateSuspendFunction
import io.github.resilience4j.kotlin.retry.decorateSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.decorateSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import kotlin.reflect.KClass

/**
 * Resilience4j 의 components를 suspend 함수에 대응하도록 decorate 합니다.
 *
 * @see [io.github.resilience4j.decorators.Decorators]
 */
object SuspendDecorators: KLoggingChannel() {

    /**
     * `suspend () -> Unit` 에 대해 resilience4j compoenents를 decorate 합니다
     *
     * ```
     * CoDecorators.ofRunnable {
     *      // 실행할 suspend 함수
     *      delay(1000)
     *      println("Hello, World!")
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke()
     * ```
     *
     * @param runnable 수행할 suspend 함수
     */
    fun ofRunnable(runnable: suspend () -> Unit): DecoratorForSuspendSupplier<Unit> {
        return DecoratorForSuspendSupplier(runnable)
    }

    /**
     * `suspend () -> T` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```
     * val result = CoDecorators.ofSupplier {
     *      // 실행할 suspend 함수
     *      delay(1000)
     *      42
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke()  // 42
     * ```
     *
     * @param T return type
     * @param supplier Suspendable supplier
     */
    fun <T> ofSupplier(supplier: suspend () -> T): DecoratorForSuspendSupplier<T> {
        return DecoratorForSuspendSupplier(supplier)
    }

    /**
     * `suspend (T) -> Unit` 에 대해 resilience4j components를 decorate 합니다
     *
     * ```
     * CoDecorators.ofConsumer<Int> { input ->
     *     // 실행할 suspend 함수
     *     delay(1000)
     *     input * 2
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21)  // 42
     * ```
     *
     * @param T input type
     * @param consumer Suspendable consumer
     */
    inline fun <T> ofConsumer(
        crossinline consumer: suspend (T) -> Unit,
    ): (T) -> DecoratorForSuspendSupplier<Unit> = { input: T ->
        ofRunnable { consumer(input) }
    }

    /**
     * `suspend (T) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```
     * val result = CoDecorators.ofFunction<Int, Int> { input ->
     *     // 실행할 suspend 함수
     *     delay(1000)
     *     input * 2
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21)  // 42
     * ```
     *
     * @param T input type
     * @param R return type
     * @param function Suspendable function
     */
    inline fun <T, R> ofFunction(
        crossinline function: suspend (T) -> R,
    ): (T) -> DecoratorForSuspendSupplier<R> = { input: T ->
        ofSupplier { function(input) }
    }

    /**
     * `suspend (T) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```
     * val result = CoDecorators.ofFunction1<Int, Int> { input ->
     *     // 실행할 suspend 함수
     *     delay(1000)
     *     input * 2
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21)  // 42
     * ```
     *
     * @param T input type
     * @param R return type
     * @param function Suspendable function
     */
    fun <T, R> ofFunction1(
        function: suspend (T) -> R,
    ): DecoratorForSuspendFunction1<T, R> {
        return DecoratorForSuspendFunction1(function)
    }

    /**
     * `suspend (T, U) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```
     * val result = CoDecorators.ofFunction2<Int, Int, Int> { a, b ->
     *     // 실행할 suspend 함수
     *     delay(1000)
     *     a * b
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21, 2)  // 42
     * ```
     *
     * @param T input type
     * @param R return type
     * @param function Suspendable function
     */
    fun <T, U, R> ofFunction2(
        function: suspend (T, U) -> R,
    ): DecoratorForSuspendFunction2<T, U, R> {
        return DecoratorForSuspendFunction2(function)
    }

    /**
     * `suspend (T, U) -> Unit` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```
     * val result = CoDecorators.ofFunction2<Int, Int, Int> { a, b ->
     *     // 실행할 suspend 함수
     *     delay(1000)
     *     println(a * b)
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21, 2)  // 42
     * ```
     *
     * @param T input type
     * @param R return type
     * @param function Suspendable function
     */
    fun <T, U> ofBiConsumer(consumer: suspend (T, U) -> Unit): DecoratorForSuspendFunction2<T, U, Unit> {
        return DecoratorForSuspendFunction2(consumer)
    }


    class DecoratorForSuspendSupplier<T>(private var supplier: suspend () -> T) {

        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            supplier = circuitBreaker.decorateSuspendFunction(supplier)
        }

        fun withRetry(retry: Retry) = apply {
            supplier = retry.decorateSuspendFunction(supplier)
        }

        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            supplier = rateLimiter.decorateSuspendFunction(supplier)
        }

        fun withBulkhead(bulkhead: Bulkhead) = apply {
            supplier = bulkhead.decorateSuspendFunction(supplier)
        }

        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            supplier = timeLimiter.decorateSuspendFunction(supplier)
        }

        fun <K> withCache(cache: Cache<K, T>): DecoratorForSuspendFunction1<K, T> {
            return SuspendDecorators.ofFunction1(cache.decorateSuspendFunction { supplier() })
        }

        fun <K> withCoroutineCache(cache: Cache<K, T>): DecoratorForSuspendFunction1<K, T> {
            return SuspendDecorators.ofFunction1(cache.decorateSuspendFunction { supplier() })
        }

        fun withFallback(handler: suspend (T?, Throwable?) -> T) = apply {
            supplier = supplier.andThen(handler)
        }

        fun withFallback(
            resultPredicate: suspend (T) -> Boolean,
            resultHandler: suspend (T) -> T,
        ) = apply {
            supplier = supplier.recover(resultPredicate, resultHandler)
        }

        fun withFallback(exceptionHandler: suspend (Throwable?) -> T) = apply {
            supplier = supplier.recover(exceptionHandler)
        }

        fun withFallback(
            exceptionType: KClass<Throwable>,
            exceptionHandler: suspend (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionType, exceptionHandler)
        }


        fun withFallback(
            exceptionTypes: Iterable<Class<Throwable>>,
            exceptionHandler: suspend (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionTypes, exceptionHandler)
        }

        fun decoreate(): suspend () -> T = supplier

        suspend fun get(): T = supplier()

        suspend fun invoke(): T = supplier()
    }

    class DecoratorForSuspendFunction1<T, R>(private var func: suspend (T) -> R) {

        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            func = circuitBreaker.decorateSuspendFunction1(func)
        }

        fun withRetry(retry: Retry) = apply {
            func = retry.decorateSuspendFunction1(func)
        }

        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            func = rateLimiter.decorateSuspendFunction1(func)
        }

        fun withBulkhead(bulkhead: Bulkhead) = apply {
            func = bulkhead.decorateSuspendFunction1(func)
        }

        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            func = timeLimiter.decorateSuspendFunction1(func)
        }

        /**
         * Cache를 제공합니다
         */
        fun withCache(cache: Cache<T, R>) = apply {
            func = cache.decorateSuspendFunction(func)
        }

        fun withSuspendCache(suspendCache: SuspendCache<T, R>) = apply {
            func = suspendCache.decorateSuspendFunction(func)
        }

        fun decoreate(): suspend (T) -> R = func

        suspend fun invoke(input: T): R = func(input)
    }

    class DecoratorForSuspendFunction2<T, U, R>(private var func: suspend (T, U) -> R) {

        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            func = circuitBreaker.decorateSuspendBiFunction(func)
        }

        fun withRetry(retry: Retry) = apply {
            func = retry.decorateSuspendBiFunction(func)
        }

        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            func = rateLimiter.decorateSuspendBiFunction(func)
        }

        fun withBulkhead(bulkhead: Bulkhead) = apply {
            func = bulkhead.decorateSuspendBiFunction(func)
        }

        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            func = timeLimiter.decorateSuspendBiFunction(func)
        }

        fun decoreate(): suspend (T, U) -> R = func

        suspend fun invoke(t: T, u: U): R = func(t, u)
    }
}
