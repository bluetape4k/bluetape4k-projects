package io.bluetape4k.resilience4j

import io.github.resilience4j.core.CallableUtils
import java.util.concurrent.Callable
import kotlin.reflect.KClass

/**
 * [Callable] 실행 후 [resultHandler]를 실행한다
 *
 * ```
 * val callable = Callable { 42 }
 * val result = callable.andThen { it + 1 }.call()  // 43
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param <R> [resultHandler]의 반환 타입
 * @param resultHandler [Callable] 실행 후 실행할 함수
 * @return [Callable] 실행 후 [resultHandler]를 실행하는 [Callable]
 */
inline fun <T: Any, R: Any> Callable<T>.andThen(
    crossinline resultHandler: (result: T) -> R,
): Callable<R> {
    return CallableUtils.andThen(this) { result: T -> resultHandler(result) }
}

/**
 * [Callable] 실행 후 [handler]를 실행한다
 *
 * ```
 * val callable = Callable { 42 }
 * val result = callable.andThen { result, error -> result + 1 }.call()  // 43
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param <R> [handler]의 반환 타입
 * @param handler [Callable] 실행 후 실행할 함수
 * @return [Callable] 실행 후 [handler]를 실행하는 [Callable]
 */
inline fun <T: Any, R: Any> Callable<T>.andThen(
    crossinline handler: (result: T, error: Throwable?) -> R,
): Callable<R> {
    return CallableUtils.andThen(this) { result: T, error: Throwable? ->
        handler(result, error)
    }
}

/**
 * [Callable] 실행 후 [resultHandler]를 실행하거나 [exceptionHandler]를 실행한다
 *
 * ```
 * val callable = Callable { 42 }
 * val result = callable.andThen({ it + 1 }, { -1 }).call()  // 43
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param <R> [resultHandler]의 반환 타입
 * @param resultHandler [Callable] 실행 후 실행할 함수
 * @param exceptionHandler [Callable] 실행 중 예외 발생 시 실행할 함수
 * @return [Callable] 실행 후 [resultHandler]를 실행하거나 [exceptionHandler]를 실행하는 [Callable]
 */
inline fun <T: Any, R: Any> Callable<T>.andThen(
    crossinline resultHandler: (T) -> R,
    crossinline exceptionHandler: (Throwable) -> R,
): Callable<R> {
    return CallableUtils.andThen(
        this,
        { result: T -> resultHandler(result) },
        { error: Throwable -> exceptionHandler(error) })
}

/**
 * [Callable] 실행 시 예외가 발생하면 [exceptionHandler]를 실행한다
 *
 * ```
 * val callable = Callable { throw RuntimeException() }
 * val result = callable.recover { -1 }.call()  // -1
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param exceptionHandler 예외 발생 시 실행할 함수
 * @return [Callable] 실행 시 예외가 발생하면 [exceptionHandler]를 실행하는 [Callable]
 */
inline fun <T: Any> Callable<T>.recover(
    crossinline exceptionHandler: (Throwable) -> T,
): Callable<T> {
    return CallableUtils.recover(this) { error: Throwable ->
        exceptionHandler(error)
    }
}

/**
 * [Callable] 실행 후 [resultPredicate]에 따라 [resultHandler]를 실행한다
 *
 * ```
 * val callable = Callable { 42 }
 * val result = callable.recover({ it == 42 }, { it + 1 }).call()  // 43
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param resultPredicate [Callable] 실행 결과를 검사할 함수
 * @param resultHandler [resultPredicate]에 따라 실행할 함수
 * @return [Callable] 실행 후 [resultPredicate]에 따라 [resultHandler]를 실행하는 [Callable]
 */
inline fun <T: Any> Callable<T>.recover(
    crossinline resultPredicate: (T) -> Boolean,
    crossinline resultHandler: (T) -> T,
): Callable<T> {
    return CallableUtils.recover(this, { resultPredicate.invoke(it) }) { result: T ->
        resultHandler(result)
    }
}

/**
 * [Callable] 실행 후 [exceptionTypes]의 예외가 발생하면 [exceptionHandler]를 실행한다
 *
 * ```
 * val callable = Callable { throw RuntimeException() }
 * val result = callable.recover(listOf(RuntimeException::class.java)) { -1 }.call()  // -1
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param exceptionTypes 예외 타입 목록
 * @param exceptionHandler 예외 발생 시 실행할 함수
 * @return [Callable] 실행 후 [exceptionTypes]에 따라 [exceptionHandler]를 실행하는 [Callable]
 */
inline fun <T: Any> Callable<T>.recover(
    exceptionTypes: List<Class<out Throwable>>,
    crossinline exceptionHandler: (Throwable) -> T,
): Callable<T> {
    return CallableUtils.recover(this, exceptionTypes) { error: Throwable ->
        exceptionHandler(error)
    }
}

/**
 * [Callable] 실행 후 [exceptionType]의 예외가 발생하면 [exceptionHandler]를 실행한다
 *
 * ```
 * val callable = Callable { throw RuntimeException() }
 * val result = callable.recover(RuntimeException::class.java) { -1 }.call()  // -1
 * ```
 *
 * @param <T> [Callable]의 반환 타입
 * @param <X> 예외 타입
 * @param exceptionType 예외 타입
 * @param exceptionHandler 예외 발생 시 실행할 함수
 * @return [Callable] 실행 후 [exceptionType]에 따라 [exceptionHandler]를 실행하는 [Callable]
 */
inline fun <X: Throwable, T: Any> Callable<T>.recover(
    exceptionType: KClass<X>,
    crossinline exceptionHandler: (Throwable) -> T,
): Callable<T> {
    return CallableUtils.recover(this, exceptionType.java) { error: Throwable ->
        exceptionHandler(error)
    }
}
