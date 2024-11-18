package io.bluetape4k.resilience4j.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.decorateSuspendFunction
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction

/**
 * suspend 함수 실행 시 [CircuitBreaker] 를 적용합니다.
 *
 * ```
 * val result = withCircuitBreaker(circuitBreaker) {
 *      // suspend 함수
 *      delay(10)
 *      42
 * }
 * ```
 * @param circuitBreaker CircuitBreaker 인스턴스
 * @param block suspend 함수
 * @return suspend 함수 실행 결과
 */
suspend inline fun <R> withCircuitBreaker(
    circuitBreaker: CircuitBreaker,
    crossinline block: suspend () -> R,
): R {
    return circuitBreaker.executeSuspendFunction { block() }
}

/**
 * suspend 함수 실행 시 [CircuitBreaker] 를 적용합니다.
 *
 * ```
 * val result = withCircuitBreaker(circuitBreaker, param) { param ->
 *      // suspend 함수
 *      delay(10)
 *      param
 * }
 * ```
 *
 * @param circuitBreaker CircuitBreaker 인스턴스
 * @param param suspend 함수의 파라미터
 * @param func suspend 함수
 * @return suspend 함수 실행 결과
 */
suspend inline fun <T, R> withCircuitBreaker(
    circuitBreaker: CircuitBreaker,
    param: T,
    crossinline func: suspend (T) -> R,
): R {
    return circuitBreaker.decorateSuspendFunction1(func).invoke(param)
}

/**
 * suspend 함수 실행 시 [CircuitBreaker] 를 적용합니다.
 *
 * ```
 * val result = withCircuitBreaker(circuitBreaker, param1, param2) { a, b ->
 *      // suspend 함수
 *      delay(10)
 *      a + b
 * }
 * ```
 *
 * @param circuitBreaker CircuitBreaker 인스턴스
 * @param param1 suspend 함수의 첫 번째 파라미터
 * @param param2 suspend 함수의 두 번째 파라미터
 * @param bifunc suspend 함수
 * @return suspend 함수 실행 결과
 */
suspend inline fun <T, U, R> withCircuitBreaker(
    circuitBreaker: CircuitBreaker,
    param1: T,
    param2: U,
    crossinline bifunc: suspend (T, U) -> R,
): R {
    return circuitBreaker.decorateSuspendBiFunction(bifunc).invoke(param1, param2)
}

/**
 * suspend 함수를 [CircuitBreaker] 를 적용한 함수로 변환합니다.
 *
 * ```
 * val decoratedFunc = circuitBreaker.decorateSuspendFunction1 { input: Int ->
 *      // suspend 함수
 *      delay(10)
 *      input
 * }
 *
 * val result = decoratedFunc(42)
 * ```
 *
 * @param func suspend 함수
 * @return [CircuitBreaker] 를 적용한 suspend 함수
 */
inline fun <T, R> CircuitBreaker.decorateSuspendFunction1(
    crossinline func: suspend (T) -> R,
): suspend (T) -> R = { input: T ->
    decorateSuspendFunction { func(input) }.invoke()
}

/**
 * suspend 함수를 [CircuitBreaker] 를 적용한 함수로 변환합니다.
 *
 * ```
 * val decoratedFunc = circuitBreaker.decorateSuspendBiFunction { a: Int, b: Int ->
 *      // suspend 함수
 *      delay(10)
 *      a + b
 * }
 *
 * val result = decoratedFunc(10, 20)
 * ```
 *
 * @param bifunc suspend 함수
 * @return [CircuitBreaker] 를 적용한 suspend 함수
 */
inline fun <T, U, R> CircuitBreaker.decorateSuspendBiFunction(
    crossinline bifunc: suspend (T, U) -> R,
): suspend (T, U) -> R = { t: T, u: U ->
    decorateSuspendFunction { bifunc(t, u) }.invoke()
}
