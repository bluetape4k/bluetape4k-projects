package io.bluetape4k.resilience4j.retry

import io.bluetape4k.resilience4j.rethrowIfCancellation
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.delay

/**
 * Executes [block] through [retry].
 *
 * Cancellation is not part of retry policy evaluation. A [kotlin.coroutines.cancellation.CancellationException]
 * thrown by [block] or by the retry delay is rethrown immediately and is never retried.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry) {
 *     callRemoteService()
 * }
 * ```
 *
 * @param retry retry policy to apply
 * @param block suspend operation to execute
 * @return the successful operation result
 */
suspend inline fun <R: Any> withRetry(
    retry: Retry,
    crossinline block: suspend () -> R,
): R {
    return retry.executeSuspendFunctionPreservingCancellation { block() }
}

/**
 * Executes [func] for [input] through [retry].
 *
 * Coroutine cancellation is rethrown immediately and is never retried.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry, 21) { input ->
 *     input * 2
 * }
 * ```
 *
 * @param retry retry policy to apply
 * @param input function input
 * @param func suspend function to execute
 * @return the successful function result
 */
suspend inline fun <T: Any, R: Any> withRetry(
    retry: Retry,
    input: T,
    crossinline func: suspend (T) -> R,
): R {
    return retry.decorateSuspendFunction1(func).invoke(input)
}

/**
 * Executes [bifunc] for [param1] and [param2] through [retry].
 *
 * Coroutine cancellation is rethrown immediately and is never retried.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val result = withRetry(retry, 21, 21) { input1, input2 ->
 *     input1 + input2
 * }
 * ```
 *
 * @param retry retry policy to apply
 * @param param1 first function input
 * @param param2 second function input
 * @param bifunc suspend function to execute
 * @return the successful function result
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
 * Decorates [func] with this [Retry].
 *
 * Coroutine cancellation is rethrown immediately and is never retried.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val func1 = retry.decorateSuspendFunction { input ->
 *     input * 2
 * }
 * val result = func1(21)  // 42
 * ```
 *
 * @param func suspend function to decorate
 * @return decorated suspend function
 */
inline fun <T, R> Retry.decorateSuspendFunction1(
    crossinline func: suspend (input: T) -> R,
): suspend (T) -> R = { input: T ->
    this.executeSuspendFunctionPreservingCancellation { func(input) }
}

/**
 * Decorates [bifunc] with this [Retry].
 *
 * Coroutine cancellation is rethrown immediately and is never retried.
 *
 * ```
 * val retry = Retry.ofDefaults("name")
 * val bifunc = retry.decorateSuspendBiFunction { input1, input2 ->
 *     input1 + input2
 * }
 * val result = bifunc(21, 21)  // 42
 * ```
 *
 * @receiver retry policy to apply
 * @param bifunc suspend function to decorate
 * @return decorated suspend function
 */
inline fun <T, U, R> Retry.decorateSuspendBiFunction(
    crossinline bifunc: suspend (t: T, u: U) -> R,
): suspend (T, U) -> R = { t: T, u: U ->
    this.executeSuspendFunctionPreservingCancellation { bifunc(t, u) }
}

@PublishedApi
internal suspend fun <T> Retry.executeSuspendFunctionPreservingCancellation(
    block: suspend () -> T,
): T {
    val retryContext = asyncContext<Any?>()

    while (true) {
        try {
            val result = block()
            val delayMs = RetryAsyncContextBridge.onResult(retryContext, result)

            if (delayMs >= 0) {
                delay(delayMs)
                continue
            }

            retryContext.onComplete()
            return result
        } catch (e: Exception) {
            e.rethrowIfCancellation()

            val delayMs = retryContext.onError(e)
            if (delayMs >= 0) {
                delay(delayMs)
                continue
            }

            throw e
        }
    }
}
