package io.bluetape4k.resilience4j.ratelimiter

import io.github.resilience4j.kotlin.ratelimiter.decorateSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter

/**
 * suspend 함수 [block]을 실행할 때, [RateLimiter]를 적용합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("myRateLimiter")
 * val result: String = withRateLimiter(rateLimiter) {
 *    // Rate Limiter 적용할 코드
 *    delay(1000)
 *    "result"
 * }
 * ```
 *
 * @param rateLimiter RateLimiter
 * @param block suspend 함수
 * @return [block]의 실행 결과
 */
suspend fun <R> withRateLimiter(
    rateLimiter: RateLimiter,
    block: suspend () -> R,
): R {
    return rateLimiter.executeSuspendFunction(block)
}

/**
 * suspend 함수 [func]을 실행할 때, [RateLimiter]를 적용합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("myRateLimiter")
 * val result = withRateLimiter(rateLimiter, 21) { param ->
 *     // Rate Limiter 적용할 코드
 *     delay(1000)
 *     param * 2
 * } // result is 42
 * ```
 *
 * @param rateLimiter RateLimiter
 * @param param 함수 파라미터
 * @param func suspend 함수
 */
suspend fun <T, R> withRateLimiter(
    rateLimiter: RateLimiter,
    param: T,
    func: suspend (T) -> R,
): R {
    return rateLimiter.decorateSuspendFunction1(func).invoke(param)
}

/**
 * suspend 함수 [bifunc]을 실행할 때, [RateLimiter]를 적용합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("myRateLimiter")
 * val result = withRateLimiter(rateLimiter, 21, 21) { param1, param2 ->
 *    // Rate Limiter 적용할 코드
 *    delay(1000)
 *    param1 + param2
 * } // result is 42
 */
suspend fun <T, U, R> withRateLimiter(
    rateLimiter: RateLimiter,
    param1: T,
    param2: U,
    bifunc: suspend (T, U) -> R,
): R {
    return rateLimiter.decorateSuspendBiFunction(bifunc).invoke(param1, param2)
}

/**
 * suspend 함수 [func]을 RateLimiter를 적용하여 실행합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("myRateLimiter")
 * val func = rateLimiter.decorateSuspendFunction1 { param ->
 *   // Rate Limiter 적용할 코드
 *   delay(1000)
 *   param * 2
 * }
 * val result = func(21) // result is 42
 * ```
 *
 * @param func suspend 함수
 * @return suspend 함수 실행 결과
 */
inline fun <T, R> RateLimiter.decorateSuspendFunction1(
    crossinline func: suspend (T) -> R,
): suspend (T) -> R = { input ->
    decorateSuspendFunction { func(input) }.invoke()
}

/**
 * suspend 함수 [bifunc]을 RateLimiter를 적용하여 실행합니다.
 *
 * ```
 * val rateLimiter = RateLimiter.ofDefaults("myRateLimiter")
 * val bifunc = rateLimiter.decorateSuspendBiFunction { param1, param2 ->
 *   // Rate Limiter 적용할 코드
 *   delay(1000)
 *   param1 + param2
 * }
 * val result = bifunc(21, 21) // result is 42
 * ```
 *
 * @param bifunc suspend 함수
 * @return suspend 함수 실행 결과
 */
fun <T, U, R> RateLimiter.decorateSuspendBiFunction(
    bifunc: suspend (T, U) -> R,
): suspend (T, U) -> R = { t: T, u: U ->
    decorateSuspendFunction { bifunc(t, u) }.invoke()
}
