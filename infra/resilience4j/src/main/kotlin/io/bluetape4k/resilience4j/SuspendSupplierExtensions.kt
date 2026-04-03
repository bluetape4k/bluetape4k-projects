package io.bluetape4k.resilience4j

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

@PublishedApi
internal fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

/**
 * suspend 함수의 결과를 [resultHandler]로 변환합니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { 42 }
 * val mapped = fn.andThen { it.toString() }
 * // mapped() == "42"
 * ```
 *
 * @param T 원본 반환 타입
 * @param R 변환 후 반환 타입
 * @param resultHandler 변환 함수
 * @return 변환된 결과를 반환하는 suspend 함수
 */
inline fun <T, R> (suspend () -> T).andThen(
    crossinline resultHandler: suspend (T) -> R,
): suspend () -> R = {
    resultHandler(this.invoke())
}

/**
 * suspend 함수 실행 결과 또는 예외를 [handler]로 처리합니다.
 *
 * 성공 시 `handler(result, null)`, 실패 시 `handler(null, exception)` 형태로 호출됩니다.
 * 코루틴 취소는 복구하지 않고 그대로 전파합니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { throw RuntimeException("err") }
 * val safe = fn.andThen { result, ex ->
 *     if (ex != null) -1 else result!!
 * }
 * // safe() == -1
 * ```
 *
 * @param T 원본 반환 타입
 * @param R 변환 후 반환 타입
 * @param handler 결과 또는 예외를 받는 핸들러
 * @return 처리된 결과를 반환하는 suspend 함수
 */
inline fun <T, R> (suspend () -> T).andThen(
    crossinline handler: suspend (T?, Throwable?) -> R,
): (suspend () -> R) = {
    try {
        val result = this()
        handler(result, null)
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        handler(null, e)
    }
}

/**
 * suspend 함수의 결과와 예외를 각각의 핸들러로 처리합니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { 42 }
 * val mapped = fn.andThen(
 *     resultHandler = { it.toString() },
 *     exceptionHandler = { "error" }
 * )
 * // mapped() == "42"
 * ```
 *
 * @param T 원본 반환 타입
 * @param R 변환 후 반환 타입
 * @param resultHandler 성공 시 호출할 핸들러
 * @param exceptionHandler 예외 발생 시 호출할 핸들러
 * @return 처리된 결과를 반환하는 suspend 함수
 */
inline fun <T, R> (suspend () -> T).andThen(
    crossinline resultHandler: suspend (T) -> R,
    crossinline exceptionHandler: suspend (Throwable?) -> R,
): (suspend () -> R) = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        exceptionHandler(e)
    }
}

/**
 * suspend 함수 실행 중 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { throw RuntimeException("err") }
 * val safe = fn.recover { -1 }
 * // safe() == -1
 * ```
 *
 * 코루틴 취소는 복구하지 않고 그대로 전파합니다.
 *
 * @param T 반환 타입
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 예외 복구 로직이 포함된 suspend 함수
 */
inline fun <T> (suspend () -> T).recover(
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        exceptionHandler(e)
    }
}

/**
 * suspend 함수의 결과가 [resultPredicate]를 만족하면 [resultHandler]로 대체 값을 반환합니다.
 *
 * ```kotlin
 * val fn: suspend () -> String = { "" }
 * val safe = fn.recover(
 *     resultPredicate = { it.isEmpty() },
 *     resultHandler = { "default" }
 * )
 * // safe() == "default"
 * ```
 *
 * @param T 반환 타입
 * @param resultPredicate 결과를 검사하는 조건식
 * @param resultHandler 조건 충족 시 대체 값을 반환하는 핸들러
 * @return 결과 복구 로직이 포함된 suspend 함수
 */
inline fun <T> (suspend () -> T).recover(
    crossinline resultPredicate: suspend (T) -> Boolean,
    crossinline resultHandler: suspend (T) -> T,
): suspend () -> T = {
    val result = this.invoke()

    if (resultPredicate(result)) {
        resultHandler(result)
    } else {
        result
    }
}

/**
 * suspend 함수 실행 중 [exceptionType]에 해당하는 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 * 다른 타입의 예외는 그대로 rethrow 됩니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { throw IOException("err") }
 * val safe = fn.recover(IOException::class) { -1 }
 * // safe() == -1
 * ```
 *
 * @param T 반환 타입
 * @param X 처리할 예외 타입
 * @param exceptionType 처리할 예외 타입의 KClass
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 특정 예외 복구 로직이 포함된 suspend 함수
 */
inline fun <X: Throwable, T> (suspend () -> T).recover(
    exceptionType: KClass<X>,
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        if (exceptionType.java.isAssignableFrom(e.javaClass)) {
            exceptionHandler(e)
        } else {
            throw e
        }
    }
}

/**
 * suspend 함수 실행 중 [exceptionTypes] 목록에 해당하는 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 * 목록에 없는 타입의 예외는 그대로 rethrow 됩니다.
 *
 * ```kotlin
 * val fn: suspend () -> Int = { throw IOException("err") }
 * val safe = fn.recover(listOf(IOException::class.java)) { -1 }
 * // safe() == -1
 * ```
 *
 * @param T 반환 타입
 * @param X 처리할 예외 타입
 * @param exceptionTypes 처리할 예외 타입 목록
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 복수 예외 복구 로직이 포함된 suspend 함수
 */
inline fun <X: Throwable, T> (suspend () -> T).recover(
    exceptionTypes: Iterable<Class<X>>,
    crossinline exceptionHandler: suspend (Throwable?) -> T,
): (suspend () -> T) = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        e.rethrowIfCancellation()
        if (exceptionTypes.any { it.isAssignableFrom(e.javaClass) }) {
            exceptionHandler(e)
        } else {
            throw e
        }
    }
}
