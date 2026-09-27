package io.bluetape4k.support

import io.bluetape4k.concurrent.get
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

internal const val MIN_TIMEOUT_MILLIS = 10L

/**
 * 제한시간을 두고 [action]을 비동기로 실행합니다. 제한시간이 지나면 Exception을 가지는 [CompletableFuture]를 반환합니다.
 *
 * 참고: [Asynchronous timeouts with CompletableFutures in Java 8 and Java 9](http://iteratrlearning.com/java9/2016/09/13/java9-timeouts-completablefutures.html)
 *
 * Timeout 전에 완료될 때:
 * ```kotlin
 * val future = asyncRunWithTimeout(1000) {
 *     Thread.sleep(100)
 * }
 * future.get() // 완료되어야 함
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```kotlin
 * assertFailsWith<ExecutionException> {
 *     asyncRunWithTimeout(500) {
 *         Thread.sleep(1000)
 *     }.get()
 * }.cause shouldBeInstanceOf TimeoutException::class
 * ```
 *
 * 시간 초과가 발생하면 반환된 future를 예외 완료하고 내부 실행기를 종료합니다.
 * 이미 제출한 [action]은 취소하거나 interrupt하지 않으므로 제한 시간을 넘긴 뒤에도 실행될 수 있습니다.
 *
 * @param timeoutMillis 제한 시간
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면
 * [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
 */
fun <T> asyncRunWithTimeout(timeoutMillis: Long, action: () -> T): CompletableFuture<T> =
    asyncRunWithTimeout(timeoutMillis, Executors.newVirtualThreadPerTaskExecutor(), action)

/**
 * [executor]를 사용해 [action]을 비동기로 실행합니다. 완료 후 실행기를 종료합니다.
 * 이미 제출한 [action]은 시간 초과 뒤에도 계속 실행될 수 있습니다.
 *
 * @param timeoutMillis 제한 시간
 * @param executor 완료 후 종료할 실행기입니다. 호출 시 소유권을 이 함수에 넘깁니다.
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture]
 */
fun <T> asyncRunWithTimeout(
    timeoutMillis: Long,
    executor: ExecutorService,
    action: () -> T,
): CompletableFuture<T> {
    return CompletableFuture
        .supplyAsync({ action() }, executor)
        .orTimeout(timeoutMillis.coerceAtLeast(MIN_TIMEOUT_MILLIS), TimeUnit.MILLISECONDS)
        .whenComplete { _, _ ->
            executor.shutdown()
        }
}

/**
 * 제한시간을 두고 [action]을 비동기로 실행합니다. 제한시간이 지나면 Exception을 가지는 [CompletableFuture]를 반환합니다.
 *
 * 참고: [Asynchronous timeouts with CompletableFutures in Java 8 and Java 9](http://iteratrlearning.com/java9/2016/09/13/java9-timeouts-completablefutures.html)
 *
 * Timeout 전에 완료될 때:
 * ```kotlin
 * val future = asyncRunWithTimeout(1000) {
 *     Thread.sleep(100)
 * }
 * future.get() // 완료되어야 함
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```kotlin
 * assertFailsWith<ExecutionException> {
 *     asyncRunWithTimeout(500) {
 *         Thread.sleep(1000)
 *     }.get()
 * }.cause shouldBeInstanceOf TimeoutException::class
 * ```
 *
 * @param timeout 제한 시간
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면
 * [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
 */
fun <T> asyncRunWithTimeout(timeout: Duration, action: () -> T): CompletableFuture<T> =
    asyncRunWithTimeout(timeout.inWholeMilliseconds, action = action)

/**
 * Timeout 내에서 [action]을 실행합니다. [action]이 [timeoutMillis] 시간 내에 종료되지 않으면 null 을 반환합니다.
 *
 * Timeout 전에 완료될 때:
 * ```kotlin
 * val result = withTimeoutOrNull(1000) {
 *     Thread.sleep(100)
 *     42
 * }
 * // result is 42
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```kotlin
 * val result = withTimeoutOrNull(500) {
 *     Thread.sleep(1000)
 *     42
 * }
 * // result is null
 * ```
 *
 * @param timeoutMillis 실행 제한 시간 (millisecond)
 * @param action 실행할 block
 * @return [action]의 실행 결과, [timeoutMillis] 시간 내에 종료되지 않으면 null
 */
inline fun <T: Any> withTimeoutOrNull(timeoutMillis: Long, crossinline action: () -> T): T? {
    return try {
        asyncRunWithTimeout(timeoutMillis) { action() }.get()
    } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause is TimeoutException) null else throw e
    }
}

/**
 * Timeout 내에서 [action]을 실행합니다. [action]이 [timeout] 시간 내에 종료되지 않으면 null 을 반환합니다.
 *
 * Timeout 전에 완료될 때:
 * ```kotlin
 * val result = withTimeoutOrNull(1000.milliseconds) {
 *     Thread.sleep(100)
 *     42
 * }
 * // result is 42
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```kotlin
 * val result = withTimeoutOrNull(500.milliseconds) {
 *     Thread.sleep(1000)
 *     42
 * }
 * // result is null
 * ```
 *
 * @param timeout 제한 시간
 * @param action 실행할 block
 * @return [action]의 실행 결과, [timeout] 시간 내에 종료되지 않으면 null
 */
inline fun <T: Any> withTimeoutOrNull(timeout: Duration, crossinline action: () -> T): T? =
    withTimeoutOrNull(timeout.inWholeMilliseconds, action)

/**
 * 각 [timeout] 제한으로 [action]을 최대 [maxRetries]번 시도하고 첫 제한 시간 내 결과를 반환합니다.
 * 모든 시도가 제한 시간을 넘으면 `null`을 반환합니다.
 *
 * 시간 초과는 이미 시작된 [action]을 중단하거나 interrupt하지 않습니다. 따라서 이전 시도가 계속 실행되는
 * 동안 다음 시도가 시작될 수 있으므로, 재시도할 작업은 중복 실행을 견딜 수 있어야 합니다.
 *
 * @throws IllegalArgumentException [maxRetries]가 1보다 작으면 발생합니다.
 */
fun <T: Any> retryWithTimeoutOrNull(
    maxRetries: Int,
    timeout: Duration,
    action: () -> T,
): T? {
    maxRetries.requirePositiveNumber("maxRetries")

    repeat(maxRetries) {
        val result = withTimeoutOrNull(timeout, action)
        if (result != null) return result
    }
    return null
}
