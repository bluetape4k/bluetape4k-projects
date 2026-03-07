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
 * Resilience4j 컴포넌트를 suspend 함수 체인으로 조합하는 데코레이터 진입점입니다.
 *
 * ## 동작/계약
 * - `ofSupplier/ofFunction*` 계열은 입력 함수를 감싼 데코레이터 빌더를 반환합니다.
 * - `withCircuitBreaker/withRetry/withRateLimit/...` 호출 순서대로 함수가 중첩됩니다.
 * - fallback 계열은 예외/결과 조건에 따라 대체 함수를 적용합니다.
 *
 * ```kotlin
 * val fn = SuspendDecorators.ofSupplier { 42 }
 *   .withRetry(retry)
 *   .decorate()
 * // fn() == 42
 * ```
 *
 * @see [io.github.resilience4j.decorators.Decorators]
 */
object SuspendDecorators: KLoggingChannel() {

    /**
     * `suspend () -> Unit` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * SuspendDecorators.ofRunnable {
     *     delay(1000)
     *     println("Hello, World!")
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke()
     * ```
     *
     * @param runnable 수행할 suspend 함수
     * @return [DecoratorForSuspendSupplier] 빌더
     */
    fun ofRunnable(runnable: suspend () -> Unit): DecoratorForSuspendSupplier<Unit> {
        return DecoratorForSuspendSupplier(runnable)
    }

    /**
     * `suspend () -> T` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofSupplier {
     *     delay(1000)
     *     42
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke()  // 42
     * ```
     *
     * @param T 반환 타입
     * @param supplier 실행할 suspend 함수
     * @return [DecoratorForSuspendSupplier] 빌더
     */
    fun <T> ofSupplier(supplier: suspend () -> T): DecoratorForSuspendSupplier<T> {
        return DecoratorForSuspendSupplier(supplier)
    }

    /**
     * `suspend (T) -> Unit` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * SuspendDecorators.ofConsumer<Int> { input ->
     *     delay(1000)
     *     println(input)
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(42)
     * ```
     *
     * @param T 입력 타입
     * @param consumer 실행할 suspend 함수
     * @return 입력을 받아 [DecoratorForSuspendSupplier]를 반환하는 함수
     */
    inline fun <T> ofConsumer(
        crossinline consumer: suspend (T) -> Unit,
    ): (T) -> DecoratorForSuspendSupplier<Unit> = { input: T ->
        ofRunnable { consumer(input) }
    }

    /**
     * `suspend (T) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofFunction<Int, Int> { input ->
     *     delay(1000)
     *     input * 2
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21)  // 42
     * ```
     *
     * @param T 입력 타입
     * @param R 반환 타입
     * @param function 실행할 suspend 함수
     * @return 입력을 받아 [DecoratorForSuspendSupplier]를 반환하는 함수
     */
    inline fun <T, R> ofFunction(
        crossinline function: suspend (T) -> R,
    ): (T) -> DecoratorForSuspendSupplier<R> = { input: T ->
        ofSupplier { function(input) }
    }

    /**
     * `suspend (T) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * `ofFunction`과 달리 [DecoratorForSuspendFunction1]를 반환하여 체인에서 `invoke(input)` 직접 호출이 가능합니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofFunction1<Int, Int> { input ->
     *     delay(1000)
     *     input * 2
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21)  // 42
     * ```
     *
     * @param T 입력 타입
     * @param R 반환 타입
     * @param function 실행할 suspend 함수
     * @return [DecoratorForSuspendFunction1] 빌더
     */
    fun <T, R> ofFunction1(
        function: suspend (T) -> R,
    ): DecoratorForSuspendFunction1<T, R> {
        return DecoratorForSuspendFunction1(function)
    }

    /**
     * `suspend (T, U) -> R` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofFunction2<Int, Int, Int> { a, b ->
     *     delay(1000)
     *     a * b
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21, 2)  // 42
     * ```
     *
     * @param T 첫 번째 입력 타입
     * @param U 두 번째 입력 타입
     * @param R 반환 타입
     * @param function 실행할 suspend 함수
     * @return [DecoratorForSuspendFunction2] 빌더
     */
    fun <T, U, R> ofFunction2(
        function: suspend (T, U) -> R,
    ): DecoratorForSuspendFunction2<T, U, R> {
        return DecoratorForSuspendFunction2(function)
    }

    /**
     * `suspend (T, U) -> Unit` 에 대해 resilience4j components를 decorate 합니다.
     *
     * ```kotlin
     * SuspendDecorators.ofBiConsumer<Int, Int> { a, b ->
     *     delay(1000)
     *     println("$a + $b = ${a + b}")
     * }
     * .withCircuitBreaker(circuitBreaker)
     * .withRetry(retry)
     * .invoke(21, 21)
     * ```
     *
     * @param T 첫 번째 입력 타입
     * @param U 두 번째 입력 타입
     * @param consumer 실행할 suspend 함수
     * @return [DecoratorForSuspendFunction2] 빌더
     */
    fun <T, U> ofBiConsumer(consumer: suspend (T, U) -> Unit): DecoratorForSuspendFunction2<T, U, Unit> {
        return DecoratorForSuspendFunction2(consumer)
    }


    /**
     * `suspend () -> T` 형태의 supplier에 Resilience4j 컴포넌트를 순차적으로 적용하는 빌더입니다.
     *
     * [withCircuitBreaker], [withRetry], [withRateLimit], [withBulkhead], [withTimeLimiter]를 조합하여
     * 복합 resilience 패턴을 구성합니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofSupplier { apiCall() }
     *     .withCircuitBreaker(circuitBreaker)
     *     .withRetry(retry)
     *     .withRateLimit(rateLimiter)
     *     .invoke()
     * ```
     *
     * @param T 반환 타입
     */
    class DecoratorForSuspendSupplier<T>(private var supplier: suspend () -> T) {

        /**
         * [CircuitBreaker]를 적용합니다.
         *
         * @param circuitBreaker 적용할 CircuitBreaker 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            supplier = circuitBreaker.decorateSuspendFunction(supplier)
        }

        /**
         * [Retry]를 적용합니다.
         *
         * @param retry 적용할 Retry 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRetry(retry: Retry) = apply {
            supplier = retry.decorateSuspendFunction(supplier)
        }

        /**
         * [RateLimiter]를 적용합니다.
         *
         * @param rateLimiter 적용할 RateLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            supplier = rateLimiter.decorateSuspendFunction(supplier)
        }

        /**
         * [Bulkhead]를 적용합니다.
         *
         * @param bulkhead 적용할 Bulkhead 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withBulkhead(bulkhead: Bulkhead) = apply {
            supplier = bulkhead.decorateSuspendFunction(supplier)
        }

        /**
         * [TimeLimiter]를 적용합니다.
         *
         * @param timeLimiter 적용할 TimeLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            supplier = timeLimiter.decorateSuspendFunction(supplier)
        }

        /**
         * Resilience4j [Cache]를 적용합니다. 키를 받아 [DecoratorForSuspendFunction1]로 변환됩니다.
         *
         * @param K 캐시 키 타입
         * @param cache 적용할 Resilience4j Cache 인스턴스
         * @return 키 기반의 [DecoratorForSuspendFunction1] 빌더
         */
        fun <K> withCache(cache: Cache<K, T>): DecoratorForSuspendFunction1<K, T> {
            return SuspendDecorators.ofFunction1(cache.decorateSuspendFunction { supplier() })
        }

        /**
         * 결과와 예외를 모두 처리하는 fallback을 적용합니다.
         *
         * @param handler 결과(`T?`)와 예외(`Throwable?`)를 받아 대체 값을 반환하는 핸들러
         * @return 현재 빌더 (체인 지원)
         */
        fun withFallback(handler: suspend (T?, Throwable?) -> T) = apply {
            supplier = supplier.andThen(handler)
        }

        /**
         * 결과 조건에 따른 fallback을 적용합니다. [resultPredicate]가 true이면 [resultHandler]로 대체됩니다.
         *
         * @param resultPredicate 결과를 검사하는 조건식
         * @param resultHandler 조건 충족 시 대체 값을 반환하는 핸들러
         * @return 현재 빌더 (체인 지원)
         */
        fun withFallback(
            resultPredicate: suspend (T) -> Boolean,
            resultHandler: suspend (T) -> T,
        ) = apply {
            supplier = supplier.recover(resultPredicate, resultHandler)
        }

        /**
         * 예외 발생 시 [exceptionHandler]로 대체 값을 반환하는 fallback을 적용합니다.
         *
         * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
         * @return 현재 빌더 (체인 지원)
         */
        fun withFallback(exceptionHandler: suspend (Throwable?) -> T) = apply {
            supplier = supplier.recover(exceptionHandler)
        }

        /**
         * 특정 예외 타입에 대한 fallback을 적용합니다. 다른 예외는 그대로 rethrow 됩니다.
         *
         * @param X 처리할 예외 타입
         * @param exceptionType 처리할 예외 타입의 KClass
         * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
         * @return 현재 빌더 (체인 지원)
         */
        fun <X: Throwable> withFallback(
            exceptionType: KClass<X>,
            exceptionHandler: suspend (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionType, exceptionHandler)
        }

        /**
         * 복수의 예외 타입에 대한 fallback을 적용합니다. 목록에 없는 예외는 그대로 rethrow 됩니다.
         *
         * @param X 처리할 예외 타입
         * @param exceptionTypes 처리할 예외 타입 목록
         * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
         * @return 현재 빌더 (체인 지원)
         */
        fun <X: Throwable> withFallback(
            exceptionTypes: Iterable<Class<X>>,
            exceptionHandler: suspend (Throwable?) -> T,
        ) = apply {
            supplier = supplier.recover(exceptionTypes, exceptionHandler)
        }

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @return 조합된 suspend 함수
         */
        fun decorate(): suspend () -> T = supplier

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @deprecated [decorate]를 사용하세요. 이 함수는 오타였습니다.
         */
        @Deprecated("오타 수정됨. decorate()를 사용하세요.", ReplaceWith("decorate()"))
        fun decoreate(): suspend () -> T = supplier

        /**
         * 조합된 suspend 함수를 실행하고 결과를 반환합니다.
         *
         * @return 실행 결과
         */
        suspend fun get(): T = supplier()

        /**
         * 조합된 suspend 함수를 실행하고 결과를 반환합니다.
         *
         * @return 실행 결과
         */
        suspend fun invoke(): T = supplier()
    }

    /**
     * `suspend (T) -> R` 형태의 단일 파라미터 함수에 Resilience4j 컴포넌트를 순차적으로 적용하는 빌더입니다.
     *
     * ```kotlin
     * val decorated = SuspendDecorators.ofFunction1 { id: String ->
     *     userRepository.findById(id)
     * }
     *     .withCircuitBreaker(circuitBreaker)
     *     .withRetry(retry)
     *     .decorate()
     *
     * val user = decorated("user-1")
     * ```
     *
     * @param T 입력 타입
     * @param R 반환 타입
     */
    class DecoratorForSuspendFunction1<T, R>(private var func: suspend (T) -> R) {

        /**
         * [CircuitBreaker]를 적용합니다.
         *
         * @param circuitBreaker 적용할 CircuitBreaker 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            func = circuitBreaker.decorateSuspendFunction1(func)
        }

        /**
         * [Retry]를 적용합니다.
         *
         * @param retry 적용할 Retry 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRetry(retry: Retry) = apply {
            func = retry.decorateSuspendFunction1(func)
        }

        /**
         * [RateLimiter]를 적용합니다.
         *
         * @param rateLimiter 적용할 RateLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            func = rateLimiter.decorateSuspendFunction1(func)
        }

        /**
         * [Bulkhead]를 적용합니다.
         *
         * @param bulkhead 적용할 Bulkhead 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withBulkhead(bulkhead: Bulkhead) = apply {
            func = bulkhead.decorateSuspendFunction1(func)
        }

        /**
         * [TimeLimiter]를 적용합니다.
         *
         * @param timeLimiter 적용할 TimeLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            func = timeLimiter.decorateSuspendFunction1(func)
        }

        /**
         * Resilience4j [Cache]를 적용합니다.
         *
         * @param cache 적용할 Resilience4j Cache 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withCache(cache: Cache<T, R>) = apply {
            func = cache.decorateSuspendFunction(func)
        }

        /**
         * [SuspendCache]를 적용합니다.
         *
         * @param suspendCache 적용할 SuspendCache 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withSuspendCache(suspendCache: SuspendCache<T, R>) = apply {
            func = suspendCache.decorateSuspendFunction(func)
        }

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @return 조합된 suspend 함수
         */
        fun decorate(): suspend (T) -> R = func

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @deprecated [decorate]를 사용하세요. 이 함수는 오타였습니다.
         */
        @Deprecated("오타 수정됨. decorate()를 사용하세요.", ReplaceWith("decorate()"))
        fun decoreate(): suspend (T) -> R = func

        /**
         * 조합된 suspend 함수를 [input]과 함께 실행하고 결과를 반환합니다.
         *
         * @param input 함수 입력값
         * @return 실행 결과
         */
        suspend fun invoke(input: T): R = func(input)
    }

    /**
     * `suspend (T, U) -> R` 형태의 이중 파라미터 함수에 Resilience4j 컴포넌트를 순차적으로 적용하는 빌더입니다.
     *
     * ```kotlin
     * val result = SuspendDecorators.ofFunction2 { a: Int, b: Int ->
     *     a + b
     * }
     *     .withCircuitBreaker(circuitBreaker)
     *     .withRetry(retry)
     *     .invoke(21, 21)  // 42
     * ```
     *
     * @param T 첫 번째 입력 타입
     * @param U 두 번째 입력 타입
     * @param R 반환 타입
     */
    class DecoratorForSuspendFunction2<T, U, R>(private var func: suspend (T, U) -> R) {

        /**
         * [CircuitBreaker]를 적용합니다.
         *
         * @param circuitBreaker 적용할 CircuitBreaker 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withCircuitBreaker(circuitBreaker: CircuitBreaker) = apply {
            func = circuitBreaker.decorateSuspendBiFunction(func)
        }

        /**
         * [Retry]를 적용합니다.
         *
         * @param retry 적용할 Retry 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRetry(retry: Retry) = apply {
            func = retry.decorateSuspendBiFunction(func)
        }

        /**
         * [RateLimiter]를 적용합니다.
         *
         * @param rateLimiter 적용할 RateLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withRateLimit(rateLimiter: RateLimiter) = apply {
            func = rateLimiter.decorateSuspendBiFunction(func)
        }

        /**
         * [Bulkhead]를 적용합니다.
         *
         * @param bulkhead 적용할 Bulkhead 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withBulkhead(bulkhead: Bulkhead) = apply {
            func = bulkhead.decorateSuspendBiFunction(func)
        }

        /**
         * [TimeLimiter]를 적용합니다.
         *
         * @param timeLimiter 적용할 TimeLimiter 인스턴스
         * @return 현재 빌더 (체인 지원)
         */
        fun withTimeLimiter(timeLimiter: TimeLimiter) = apply {
            func = timeLimiter.decorateSuspendBiFunction(func)
        }

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @return 조합된 suspend 함수
         */
        fun decorate(): suspend (T, U) -> R = func

        /**
         * 현재까지 적용된 모든 데코레이터를 조합한 suspend 함수를 반환합니다.
         *
         * @deprecated [decorate]를 사용하세요. 이 함수는 오타였습니다.
         */
        @Deprecated("오타 수정됨. decorate()를 사용하세요.", ReplaceWith("decorate()"))
        fun decoreate(): suspend (T, U) -> R = func

        /**
         * 조합된 suspend 함수를 [t], [u]와 함께 실행하고 결과를 반환합니다.
         *
         * @param t 첫 번째 입력값
         * @param u 두 번째 입력값
         * @return 실행 결과
         */
        suspend fun invoke(t: T, u: U): R = func(t, u)
    }
}
