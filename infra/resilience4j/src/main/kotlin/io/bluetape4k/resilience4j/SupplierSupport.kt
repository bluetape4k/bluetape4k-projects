package io.bluetape4k.resilience4j

import kotlin.reflect.KClass

/**
 * `() -> T` 함수의 결과를 [resultHandler]로 변환합니다.
 *
 * ```kotlin
 * val fn = { 42 }
 * val mapped = fn.andThen { it.toString() }
 * // mapped() == "42"
 * ```
 *
 * @param T 원본 반환 타입
 * @param R 변환 후 반환 타입
 * @param resultHandler 변환 함수
 * @return 변환된 결과를 반환하는 함수
 */
inline fun <T, R> (() -> T).andThen(
    crossinline resultHandler: (result: T) -> R,
): () -> R = {
    resultHandler.invoke(this.invoke())
}

/**
 * `() -> T` 함수의 결과 또는 예외를 [handler]로 처리합니다.
 *
 * ```kotlin
 * val fn = { throw RuntimeException("err") }
 * val safe = fn.andThen { result, ex ->
 *     if (ex != null) -1 else result!!
 * }
 * // safe() == -1
 * ```
 *
 * @param T 원본 반환 타입
 * @param R 변환 후 반환 타입
 * @param handler 결과 또는 예외를 처리하는 함수
 * @return 처리된 결과를 반환하는 함수
 */
inline fun <T, R> (() -> T).andThen(
    crossinline handler: (result: T?, error: Throwable?) -> R,
): () -> R = {
    try {
        val result = this.invoke()
        handler.invoke(result, null)
    } catch (e: Exception) {
        handler.invoke(null, e)
    }
}

/**
 * `() -> T` 함수의 결과와 예외를 각각의 핸들러로 처리합니다.
 *
 * ```kotlin
 * val fn = { 42 }
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
 * @return 처리된 결과를 반환하는 함수
 */
inline fun <T, R> (() -> T).andThen(
    crossinline resultHandler: (T) -> R,
    crossinline exceptionHandler: (Throwable?) -> R,
): () -> R = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Exception) {
        exceptionHandler.invoke(e)
    }
}

/**
 * `() -> T` 함수 실행 시 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 *
 * ```kotlin
 * val fn = { throw RuntimeException("err") }
 * val safe = fn.recover { -1 }
 * // safe() == -1
 * ```
 *
 * @param T 반환 타입
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 예외 복구 로직이 포함된 함수
 */
@JvmName("recoverWithExceptionHandler")
inline fun <T> (() -> T).recover(crossinline exceptionHandler: (Throwable?) -> T): () -> T = {
    try {
        this.invoke()
    } catch (e: Exception) {
        exceptionHandler.invoke(e)
    }
}

/**
 * `() -> T` 함수의 결과가 [resultPredicatoe]를 만족하면 [resultHandler]로 대체 값을 반환합니다.
 *
 * ```kotlin
 * val fn = { "" }
 * val safe = fn.recover(
 *     resultPredicatoe = { it.isEmpty() },
 *     resultHandler = { "default" }
 * )
 * // safe() == "default"
 * ```
 *
 * @param T 반환 타입
 * @param resultPredicatoe 결과를 검사하는 조건식
 * @param resultHandler 조건 충족 시 대체 값을 반환하는 핸들러
 * @return 결과 복구 로직이 포함된 함수
 */
@JvmName("recoverWithResultHandler")
inline fun <T> (() -> T).recover(
    crossinline resultPredicatoe: (T) -> Boolean,
    crossinline resultHandler: (T) -> T,
): () -> T = {
    val result = this.invoke()

    if (resultPredicatoe.invoke(result)) {
        resultHandler.invoke(result)
    } else {
        result
    }
}

/**
 * `() -> T` 함수 실행 시 [exceptionType]에 해당하는 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 * 다른 타입의 예외는 그대로 rethrow 됩니다.
 *
 * ```kotlin
 * val fn = { throw IOException("err") }
 * val safe = fn.recover(IOException::class) { -1 }
 * // safe() == -1
 * ```
 *
 * @param T 반환 타입
 * @param X 처리할 예외 타입
 * @param exceptionType 처리할 예외 타입의 KClass
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 특정 예외 복구 로직이 포함된 함수
 */
@JvmName("recoverWithExceptionType")
inline fun <X: Throwable, T> (() -> T).recover(
    exceptionType: KClass<X>,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionType.java.isAssignableFrom(e.javaClass)) {
            exceptionHandler.invoke(e)
        } else {
            throw e
        }
    }
}

/**
 * `() -> T` 함수 실행 시 [exceptionTypes] 목록에 해당하는 예외가 발생하면 [exceptionHandler]로 대체 값을 반환합니다.
 * 목록에 없는 타입의 예외는 그대로 rethrow 됩니다.
 *
 * ```kotlin
 * val fn = { throw IOException("err") }
 * val safe = fn.recover(listOf(IOException::class.java)) { -1 }
 * // safe() == -1
 * ```
 *
 * @param T 반환 타입
 * @param exceptionTypes 처리할 예외 타입 목록
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 복수 예외 복구 로직이 포함된 함수
 */
@JvmName("recoverWithExceptionTypes")
inline fun <T> (() -> T).recover(
    exceptionTypes: Iterable<Class<out Throwable>>,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        this.invoke()
    } catch (e: Throwable) {
        if (exceptionTypes.any { it.isAssignableFrom(e.javaClass) }) {
            exceptionHandler.invoke(e)
        } else {
            throw e
        }
    }
}

/**
 * `() -> T` 함수의 결과를 [resultHandler]로 변환하거나, 예외 발생 시 [exceptionHandler]로 대체 값을 반환합니다.
 *
 * ```kotlin
 * val fn = { 42 }
 * val mapped = fn.recover(
 *     resultHandler = { it * 2 },
 *     exceptionHandler = { -1 }
 * )
 * // mapped() == 84
 * ```
 *
 * @param T 반환 타입
 * @param resultHandler 결과를 변환하는 핸들러
 * @param exceptionHandler 예외 발생 시 대체 값을 반환하는 핸들러
 * @return 결과/예외 복구 로직이 포함된 함수
 */
inline fun <T> (() -> T).recover(
    crossinline resultHandler: (T) -> T,
    crossinline exceptionHandler: (Throwable?) -> T,
): () -> T = {
    try {
        val result = this.invoke()
        resultHandler(result)
    } catch (e: Throwable) {
        exceptionHandler.invoke(e)
    }
}
