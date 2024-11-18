package io.bluetape4k.resilience4j.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.kotlin.bulkhead.decorateSuspendFunction
import io.github.resilience4j.kotlin.bulkhead.executeSuspendFunction

/**
 * suspend [block] 실행 시, Resilience4j의 Bulkhead 를 이용하여, 실행을 제어합니다.
 *
 * ```
 * val result = withBulkhead(bulkhead) {
 *    // 실행할 suspend 코드
 *    ...
 * }
 * ```
 *
 * @param bulkhead [Bulkhead] 인스턴스
 * @param block    실행할 suspend 코드
 */
suspend inline fun <R> withBulkhead(
    bulkhead: Bulkhead,
    crossinline block: suspend () -> R,
): R {
    return bulkhead.executeSuspendFunction { block() }
}

/**
 * suspend [func] 실행 시, Resilience4j의 Bulkhead 를 이용하여, 실행을 제어합니다.
 *
 * ```
 * val result = withBulkhead(bulkhead, param) { param ->
 *  // 실행할 suspend 코드
 *  ...
 * }
 * ```
 *
 * @param bulkhead [Bulkhead] 인스턴스
 * @param param    suspend 함수의 파라미터
 * @param func     실행할 suspend 함수
 * @return suspend 함수의 실행 결과
 */
suspend inline fun <T, R> withBulkhead(
    bulkhead: Bulkhead,
    param: T,
    crossinline func: suspend (T) -> R,
): R {
    return bulkhead.decorateSuspendFunction1(func).invoke(param)
}

/**
 * suspend [bifunc] 실행 시, Resilience4j의 Bulkhead 를 이용하여, 실행을 제어합니다.
 *
 * ```
 * val result = withBulkhead(bulkhead, param1, param2) { param1, param2 ->
 *  // 실행할 suspend 코드
 *  ...
 * }
 * ```
 *
 * @param bulkhead [Bulkhead] 인스턴스
 * @param param1   suspend 함수의 첫 번째 파라미터
 * @param param2   suspend 함수의 두 번째 파라미터
 * @param bifunc   실행할 suspend 함수
 * @return suspend 함수의 실행 결과
 */
suspend inline fun <T, U, R> withBulkhead(
    bulkhead: Bulkhead,
    param1: T,
    param2: U,
    crossinline bifunc: suspend (T, U) -> R,
): R {
    return bulkhead.decorateSuspendBiFunction(bifunc).invoke(param1, param2)
}

/**
 * suspend [func] 실행에 실패하는 경우, Resilience4j의 Bulkhead 를 이용하여, 실행을 제어합니다.
 *
 * ```
 * val func = bulkhead.decorateSuspendFunction { a ->
 *   // 실행할 suspend 코드
 *   ...
 * }
 * val result = func.invoke()
 * ```
 *
 * @param func Bulkhead 로 decorate 할 suspend 함수
 * @return retry로 decorated 된 suspend function
 */
inline fun <T, R> Bulkhead.decorateSuspendFunction1(
    crossinline func: suspend (input: T) -> R,
): suspend (T) -> R = { input: T ->
    this.decorateSuspendFunction { func(input) }.invoke()
}

/**
 * suspend [func] 실행에 실패하는 경우, Resilience4j의 Bulkhead 를 이용하여, 실행을 제어합니다.
 *
 * ```
 * val func = bulkhead.decorateSuspendBiFunction { a, b ->
 *  // 실행할 suspend 코드
 *  ...
 * }
 * val result = func.invoke(a, b)
 * ```
 *
 * @param func Bulkhead 로 decorate 할 suspend 함수
 * @return retry로 decorated 된 suspend function
 */
inline fun <T1, T2, R> Bulkhead.decorateSuspendBiFunction(
    crossinline func: suspend (input1: T1, input2: T2) -> R,
): suspend (T1, T2) -> R = { input1: T1, input2: T2 ->
    this.decorateSuspendFunction { func(input1, input2) }.invoke()
}
