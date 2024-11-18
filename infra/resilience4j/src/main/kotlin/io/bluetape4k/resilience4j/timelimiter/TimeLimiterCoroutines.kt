package io.bluetape4k.resilience4j.timelimiter

import io.github.resilience4j.kotlin.timelimiter.decorateSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.executeSuspendFunction
import io.github.resilience4j.timelimiter.TimeLimiter

/**
 * suspend 함수인 [block]을 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val result = withTimeLimiter(timeLimiter) {
 *     // 실행할 블록
 *     42
 * }
 * // result is 42
 * ```
 *
 * @param timeLimiter [TimeLimiter] 인스턴스
 * @param block 실행할 블록
 * @return 실행 결과
 */
suspend fun <R> withTimeLimiter(
    timeLimiter: TimeLimiter,
    block: suspend () -> R,
): R {
    return timeLimiter.executeSuspendFunction(block)
}

/**
 * suspend 함수인 [func]을 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val result = withTimeLimiter(timeLimiter, 21) {
 *     // 실행할 블록
 *     it * 2
 * }
 * // result is 42
 * ```
 *
 * @param timeLimiter [TimeLimiter] 인스턴스
 * @param param 실행할 블록의 파라미터
 * @param func 실행할 블록
 * @return 실행 결과
 */
suspend inline fun <T, R> withTimeLimiter(
    timeLimiter: TimeLimiter,
    param: T,
    crossinline func: suspend (T) -> R,
): R {
    return timeLimiter.decorateSuspendFunction1(func).invoke(param)
}

/**
 * suspend 함수인 [bifunc]을 [TimeLimiter] 를 적용하여 실행합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val result = withTimeLimiter(timeLimiter, 21, 2) { a, b ->
 *     // 실행할 블록
 *     a * b
 * }
 * // result is 42
 * ```
 *
 * @param timeLimiter [TimeLimiter] 인스턴스
 * @param param1 실행할 블록의 첫 번째 파라미터
 * @param param2 실행할 블록의 두 번째 파라미터
 * @param bifunc 실행할 블록
 * @return 실행 결과
 */
suspend inline fun <T, U, R> withTimeLimiter(
    timeLimiter: TimeLimiter,
    param1: T,
    param2: U,
    crossinline bifunc: suspend (T, U) -> R,
): R {
    return timeLimiter.decorateSuspendBiFunction(bifunc).invoke(param1, param2)
}

/**
 * suspend 함수를 [TimeLimiter] 를 적용하여 실행할 수 있도록 데코레이터를 생성합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val decorated = timeLimiter.decorateSuspendFunction1 { input ->
 *     // 실행할 블록
 *     input * 2
 * }
 * val result = decorated(21)  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param func 실행할 블록
 * @return 데코레이터
 */
inline fun <T, R> TimeLimiter.decorateSuspendFunction1(
    crossinline func: suspend (T) -> R,
): suspend (T) -> R = { input: T ->
    decorateSuspendFunction { func(input) }.invoke()
}

/**
 * suspend 함수를 [TimeLimiter] 를 적용하여 실행할 수 있도록 데코레이터를 생성합니다.
 *
 * ```
 * val timeLimiter = TimeLimiter.ofDefaults("name")
 * val decorated = timeLimiter.decorateSuspendBiFunction { a, b ->
 *     // 실행할 블록
 *     a * b
 * }
 * val result = decorated(21, 2)  // 42
 * ```
 *
 * @receiver [TimeLimiter] 인스턴스
 * @param bifunc 실행할 블록
 * @return 데코레이터
 */
inline fun <T, U, R> TimeLimiter.decorateSuspendBiFunction(
    crossinline bifunc: suspend (T, U) -> R,
): suspend (T, U) -> R = { t: T, u: U ->
    decorateSuspendFunction { bifunc(t, u) }.invoke()
}
