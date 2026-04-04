package io.bluetape4k.vertx.resilience4j

import io.vertx.core.Future
import io.vertx.core.Promise

/**
 * Vert.x [Future]가 예외를 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val future = Future.failedFuture<String>(RuntimeException("error"))
 *     .recover { "fallback" }
 * // future.result() == "fallback"
 * ```
 */
inline fun <T> Future<T>.recover(
    crossinline exceptionHandler: (Throwable?) -> T,
): Future<T> {
    return this.andThen { exceptionHandler(it.cause()) }
}

/**
 * Vert.x [Future]가 [exceptionTypes]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val future = Future.failedFuture<String>(IllegalStateException("bad"))
 *     .recover(listOf(IllegalStateException::class.java)) { "recovered" }
 * // future.result() == "recovered"
 * ```
 */
fun <T> Future<T>.recover(
    exceptionTypes: Iterable<Class<out Throwable?>>,
    exceptionHandler: (Throwable?) -> T,
): Future<T> {
    val promise = Promise.promise<T>()

    onSuccess {
        promise.complete(it)
    }
    onFailure { error ->
        tryRecover(exceptionTypes, exceptionHandler, promise, error.cause ?: error)
    }
    return promise.future()
}

/**
 * Vert.x [Future]가 [exceptionType]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val future = Future.failedFuture<String>(IllegalArgumentException("bad"))
 *     .recover(IllegalArgumentException::class.java) { "recovered" }
 * // future.result() == "recovered"
 * ```
 */
fun <T> Future<T>.recover(
    exceptionType: Class<out Throwable?>,
    exceptionHandler: (Throwable?) -> T,
): Future<T> {
    val promise = Promise.promise<T>()

    onSuccess {
        promise.complete(it)
    }
    onFailure { error ->
        tryRecover(exceptionType, exceptionHandler, promise, error.cause ?: error)
    }

    return promise.future()
}

/**
 * Vert.x [Future]가 [exceptionTypes]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 */
private fun <T> tryRecover(
    exceptionTypes: Iterable<Class<out Throwable?>>,
    exceptionHandler: (Throwable?) -> T,
    promise: Promise<T>,
    throwable: Throwable,
) {
    if (exceptionTypes.any { it.isAssignableFrom(throwable.javaClass) }) {
        try {
            promise.complete(exceptionHandler(throwable))
        } catch (fallbackException: Exception) {
            promise.fail(fallbackException)
        }
    } else {
        promise.fail(throwable)
    }
}

/**
 * Vert.x [Future]가 [exceptionType]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 */
private fun <T> tryRecover(
    exceptionType: Class<out Throwable?>,
    exceptionHandler: (Throwable?) -> T,
    promise: Promise<T>,
    throwable: Throwable,
) {
    if (exceptionType.isAssignableFrom(throwable.javaClass)) {
        try {
            promise.complete(exceptionHandler(throwable))
        } catch (fallbackException: Exception) {
            promise.fail(fallbackException)
        }
    } else {
        promise.fail(throwable)
    }
}

/**
 * Vert.x [Future]를 반환하는 함수가 예외를 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val supplier: () -> Future<String> = { Future.failedFuture(RuntimeException("error")) }
 * val recovered = supplier.recover { "fallback" }
 * // recovered().result() == "fallback"
 * ```
 */
fun <T> (() -> Future<T>).recover(exceptionHandler: (Throwable?) -> T): () -> Future<T> = {
    this.invoke().recover(exceptionHandler)
}

/**
 * Vert.x [Future]를 반환하는 함수가 완료되었을 때 [handler]를 실행합니다.
 *
 * ```kotlin
 * val supplier: () -> Future<String> = { Future.succeededFuture("ok") }
 * val mapped = supplier.recover { result, _ -> result?.uppercase() ?: "fallback" }
 * // mapped().result() == "OK"
 * ```
 */
inline fun <T, R> (() -> Future<T>).recover(
    crossinline handler: (T?, Throwable?) -> R,
): () -> Future<R> = {
    this.invoke().compose(
        { result -> Future.succeededFuture(handler(result, null)) },
        { error -> Future.succeededFuture(handler(null, error)) }
    )
}

/**
 * Vert.x [Future]를 반환하는 함수가 [exceptionType]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val supplier: () -> Future<String> = { Future.failedFuture(IllegalStateException("bad")) }
 * val recovered = supplier.recover(IllegalStateException::class.java) { "fallback" }
 * // recovered().result() == "fallback"
 * ```
 */
fun <T> (() -> Future<T>).recover(
    exceptionType: Class<out Throwable>,
    exceptionHandler: (Throwable?) -> T,
): () -> Future<T> = {
    this.invoke().recover(exceptionType, exceptionHandler)
}

/**
 * Vert.x [Future]를 반환하는 함수가 [exceptionTypes]에 해당하는 예외가 발생하는 경우 [exceptionHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val supplier: () -> Future<String> = { Future.failedFuture(IllegalStateException("bad")) }
 * val recovered = supplier.recover(listOf(IllegalStateException::class.java)) { "fallback" }
 * // recovered().result() == "fallback"
 * ```
 */
fun <T> (() -> Future<T>).recover(
    exceptionTypes: Iterable<Class<out Throwable>>,
    exceptionHandler: (Throwable?) -> T,
): () -> Future<T> = {
    this.invoke().recover(exceptionTypes, exceptionHandler)
}

/**
 * Vert.x [Future]를 반환하는 함수가 [resultPredicate]을 만족하는 결과인 경우 [resultHandler]를 실행하여 복구합니다.
 *
 * ```kotlin
 * val supplier: () -> Future<Int> = { Future.succeededFuture(-1) }
 * val recovered = supplier.recover({ it < 0 }) { 0 }
 * // recovered().result() == 0
 * ```
 */
inline fun <T> (() -> Future<T>).recover(
    crossinline resultPredicate: (T) -> Boolean,
    crossinline resultHandler: (T) -> T,
): () -> Future<T> = {
    this.invoke().map { result ->
        if (resultPredicate(result)) {
            resultHandler(result)
        } else {
            result
        }
    }
}
