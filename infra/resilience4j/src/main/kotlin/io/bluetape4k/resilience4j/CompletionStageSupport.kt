package io.bluetape4k.resilience4j

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import kotlin.reflect.KClass

/**
 * [CompletionStage] 작업 시 예외가 발생하면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: CompletionStage<Int> = ...
 * val result: Int = future.recover { 42 }.get()  // 42
 * ```
 *
 * @receiver [CompletionStage] 인스턴스
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [CompletionStage] 인스턴스
 */
fun <T> CompletionStage<T>.recover(
    exceptionHandler: (Throwable) -> T,
): CompletionStage<T> {
    return this.exceptionally(exceptionHandler)
}

/**
 * [CompletionStage] 작업 시 발생한 예외가 [exceptionTypes]에 속하면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: CompletionStage<Int> = ...
 * val result: Int = future.recover(listOf(RuntimeException::class.java)) { 42 }.get()  // 42
 * ```
 *
 * @receiver [CompletionStage] 인스턴스
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [CompletionStage] 인스턴스
 */
fun <T> CompletionStage<T>.recover(
    exceptionTypes: List<Class<out Throwable>>,
    exceptionHandler: (Throwable) -> T,
): CompletionStage<T> {
    val promise = CompletableFuture<T>()

    this.whenComplete { result, throwable ->
        if (throwable != null) {
            if (throwable is CompletionException || throwable is ExecutionException) {
                tryRecover(exceptionTypes, exceptionHandler, promise, throwable.cause!!)
            } else {
                tryRecover(exceptionTypes, exceptionHandler, promise, throwable)
            }
        } else {
            promise.complete(result)
        }
    }
    return promise
}


/**
 * [CompletionStage] 작업 시 발생한 예외가 [exceptionType]에 속하면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: CompletionStage<Int> = ...
 * val result: Int = future.recover(RuntimeException::class) { 42 }.get()  // 42
 * ```
 *
 * @receiver [CompletionStage] 인스턴스
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [CompletionStage] 인스턴스
 */
fun <X: Throwable, T> CompletionStage<T>.recover(
    exceptionType: KClass<X>,
    exceptionHandler: (Throwable?) -> T,
): CompletionStage<T> {
    val promise = CompletableFuture<T>()

    this.whenComplete { result, throwable ->
        if (throwable != null) {
            if (throwable is CompletionException || throwable is ExecutionException) {
                tryRecover(exceptionType, exceptionHandler, promise, throwable.cause!!)
            } else {
                tryRecover(exceptionType, exceptionHandler, promise, throwable)
            }
        } else {
            promise.complete(result)
        }
    }
    return promise
}

private fun <T> tryRecover(
    exceptionTypes: List<Class<out Throwable>>,
    exceptionHandler: (Throwable) -> T,
    promise: CompletableFuture<T>,
    throwable: Throwable,
) {
    if (exceptionTypes.any { it.isAssignableFrom(throwable.javaClass) }) {
        try {
            promise.complete(exceptionHandler.invoke(throwable))
        } catch (fallbackException: Exception) {
            promise.completeExceptionally(fallbackException)
        }
    } else {
        promise.completeExceptionally(throwable)
    }
}

private fun <X: Throwable, T> tryRecover(
    exceptionType: KClass<X>,
    exceptionHandler: (Throwable) -> T,
    promise: CompletableFuture<T>,
    throwable: Throwable,
) {
    if (exceptionType.java.isAssignableFrom(throwable.javaClass)) {
        try {
            promise.complete(exceptionHandler.invoke(throwable))
        } catch (fallbackException: Exception) {
            promise.completeExceptionally(fallbackException)
        }
    } else {
        promise.completeExceptionally(throwable)
    }
}

/**
 * `() -> CompletionStage<T>` 함수를 실행하고 예외가 발생하면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: () -> CompletionStage<Int> = { futureOf { 42 } }
 * val result: Int = future.recover { 42 }.invoke().get()  // 42
 * ```
 *
 * @receiver [() -> CompletionStage] 함수
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [() -> CompletionStage] 함수
 */
fun <T> (() -> CompletionStage<T>).recover(
    exceptionHandler: (Throwable?) -> T,
): () -> CompletionStage<T> = {
    this.invoke().recover(exceptionHandler)
}

/**
 * `() -> CompletionStage<T>` 함수를 실행하고 발생한 예외가 [exceptionType]이라면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: () -> CompletionStage<Int> = { futureOf { 42 } }
 * val result: Int = future.recover(RuntimeException::class) { 42 }.invoke().get()  // 42
 * ```
 *
 * @receiver [() -> CompletionStage] 함수
 * @param exceptionType 예외 타입
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [() -> CompletionStage] 함수
 */
fun <X: Throwable, T> (() -> CompletionStage<T>).recover(
    exceptionType: KClass<X>,
    exceptionHandler: (Throwable?) -> T,
): () -> CompletionStage<T> = {
    this.invoke().recover(exceptionType, exceptionHandler)
}

/**
 * `() -> CompletionStage<T>` 함수를 실행하고 발생한 예외가 [exceptionTypes] 중 하나라면 [exceptionHandler] 를 실행하여 복구합니다.
 *
 * ```
 * val future: () -> CompletionStage<Int> = { futureOf { 42 } }
 * val result: Int = future.recover(listOf(RuntimeException::class.java)) { 42 }.invoke().get()  // 42
 * ```
 *
 * @receiver [() -> CompletionStage] 함수
 * @param exceptionTypes 예외 타입 목록
 * @param exceptionHandler 예외가 발생했을 때 실행할 함수
 * @return 복구된 [() -> CompletionStage] 함수
 */
fun <T> (() -> CompletionStage<T>).recover(
    exceptionTypes: List<Class<out Throwable>>,
    exceptionHandler: (Throwable?) -> T,
): () -> CompletionStage<T> = {
    this.invoke().recover(exceptionTypes, exceptionHandler)
}

/**
 * [CompletionStage]`<T>` 함수를 실행하고 결과가 [resultPredicate]를 만족하면 [resultHandler]를 실행하여 변환합니다.
 *
 * ```
 * val future: () -> CompletionStage<Int> = { futureOf { 42 } }
 * val result: Int = future.recover({ it == 42 }, { 0 }).invoke().get()  // 0
 * ```
 *
 * @receiver [() -> CompletionStage] 함수
 * @param resultPredicate 결과를 검사할 함수
 * @param resultHandler 결과를 복구할 함수
 * @return 복구된 [() -> CompletionStage] 함수
 */
fun <T> CompletionStage<T>.recover(
    resultPredicate: (T) -> Boolean,
    resultHandler: (T) -> T,
): CompletionStage<T> {
    return this.thenApply { result ->
        if (resultPredicate(result)) {
            resultHandler(result)
        } else {
            result
        }
    }
}

/**
 * `() -> CompletionStage<T>` 함수를 실행하고 [handler]를 실행하여 변환합니다.
 *
 * ```
 * val future: () -> CompletionStage<Int> = { futureOf { 42 } }
 * val result: Int = future.andThen{result, error ->
 *    if(error != null) 0
 *    else result
 * }.invoke().get()  // 42
 * ```
 *
 * @receiver [() -> CompletionStage] 함수
 * @param handler 결과를 검사하고 복구할 함수
 * @return 복구된 [() -> CompletionStage] 함수
 */
fun <T, R> (() -> CompletionStage<T>).andThen(
    handler: (T, Throwable?) -> R,
): () -> CompletionStage<R> = {
    this.invoke().handle(handler)
}
