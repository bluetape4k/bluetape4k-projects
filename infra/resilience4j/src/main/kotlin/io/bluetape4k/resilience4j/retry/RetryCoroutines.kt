package io.bluetape4k.resilience4j.retry

import io.github.resilience4j.kotlin.retry.decorateSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry

/**
 * [block] 실행 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry) {
 *     // 실행할 작업
 *     println("Hello, World!")
 *     // 예외 발생 시, 재시도
 *     42
 * }
 * println(result)  // 42
 * ```
 *
 * @param retry Retry 설정
 * @param block 실행할 작업
 * @return 작업 결과
 */
suspend inline fun <R: Any> withRetry(
    retry: Retry,
    crossinline block: suspend () -> R,
): R {
    return retry.executeSuspendFunction { block() }
}

/**
 * [func] 실행 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry, 21) { input ->
 *     // 실행할 작업
 *     println(it)
 *     // 예외 발생 시, 재시도
 *     input * 2
 * }
 * println(result)  // 42
 * ```
 *
 * @param retry Retry 설정
 * @param input 입력
 * @param func 실행할 작업
 * @return 작업 결과
 */
suspend inline fun <T: Any, R: Any> withRetry(
    retry: Retry,
    input: T,
    crossinline func: suspend (T) -> R,
): R {
    return retry.decorateSuspendFunction1(func).invoke(input)
}

/**
 * [bifunc] 실행 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry, 21, 21) { input1, input2 ->
 *     // 실행할 작업
 *     println(it)
 *     // 예외 발생 시, 재시도
 *     input1 + input2
 * }
 * println(result)  // 42
 * ```
 *
 * @param retry Retry 설정
 * @param param1 입력1
 * @param param2 입력2
 * @param bifunc 실행할 작업
 * @return 작업 결과
 */
suspend inline fun <T: Any, U: Any, R: Any> withRetry(
    retry: Retry,
    param1: T,
    param2: U,
    crossinline bifunc: suspend (T, U) -> R,
): R {
    return retry.decorateSuspendBiFunction(bifunc).invoke(param1, param2)
}

/**
 * [func] 실행 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func1 = retry.decorateSuspendFunction { input ->
 *     // 실행할 작업
 *     println("Hello, World!")
 *     // 예외 발생 시, 재시도
 *     input * 2
 * }
 * val result = func1(21)  // 42
 * ```
 *
 * @param func 실행할 작업
 * @return 작업 결과
 */
inline fun <T, R> Retry.decorateSuspendFunction1(
    crossinline func: suspend (input: T) -> R,
): suspend (T) -> R = { input: T ->
    this.decorateSuspendFunction { func(input) }.invoke()
}

/**
 * [bifunc] 실행 시 [Retry] 를 적용합니다.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val bifunc = retry.decorateSuspendBiFunction { input1, input2 ->
 *     // 실행할 작업
 *     println("Hello, World!")
 *     // 예외 발생 시, 재시도
 *     input1 + input2
 * }
 * val result = bifunc(21, 21)  // 42
 * ```
 *
 * @receiver [Retry] 인스턴스
 * @param bifunc 실행할 작업
 * @return 작업 결과
 */
inline fun <T, U, R> Retry.decorateSuspendBiFunction(
    crossinline bifunc: suspend (t: T, u: U) -> R,
): suspend (T, U) -> R = { t: T, u: U ->
    this.decorateSuspendFunction { bifunc(t, u) }.invoke()
}
