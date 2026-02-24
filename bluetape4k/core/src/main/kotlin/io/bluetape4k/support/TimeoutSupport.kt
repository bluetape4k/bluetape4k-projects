package io.bluetape4k.support

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * 제한시간을 두고 [action]을 비동기로 실행합니다. 제한시간이 지나면 Exception을 가지는 [CompletableFuture]를 반환합니다.
 *
 * 참고: [Asynchronous timeouts with CompletableFutures in Java 8 and Java 9](http://iteratrlearning.com/java9/2016/09/13/java9-timeouts-completablefutures.html)
 *
 * Timeout 전에 완료될 때:
 * ```
 * val future = asyncRunWithTimeout(1000) {
 *     Thread.sleep(100)
 * }
 * future.get() // 완료되어야 함
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```
 * assertFailsWith<ExecutionException> {
 *     asyncRunWithTimeout(500) {
 *         Thread.sleep(1000)
 *     }.get()
 * }.cause shouldBeInstanceOf TimeoutException::class
 * ```
 *
 * @param timeoutMillis 제한 시간
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면 [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
 */
inline fun <T> asyncRunWithTimeout(
    timeoutMillis: Long,
    executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor(),
    crossinline action: () -> T,
): CompletableFuture<T> {
    return CompletableFuture
        .supplyAsync({ action() }, executor)
        .orTimeout(timeoutMillis.coerceAtLeast(10L), TimeUnit.MILLISECONDS)
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
 * ```
 * val future = asyncRunWithTimeout(1000) {
 *     Thread.sleep(100)
 * }
 * future.get() // 완료되어야 함
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```
 * assertFailsWith<ExecutionException> {
 *     asyncRunWithTimeout(500) {
 *         Thread.sleep(1000)
 *     }.get()
 * }.cause shouldBeInstanceOf TimeoutException::class
 * ```
 *
 * @param timeout 제한 시간
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면 [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
 */
inline fun <T> asyncRunWithTimeout(timeout: Duration, crossinline action: () -> T): CompletableFuture<T> {
    return asyncRunWithTimeout(timeout.inWholeMilliseconds, action = action)
}

/**
 * Timeout 내에서 [action]을 실행합니다. [action]이 [timeoutMillis] 시간 내에 종료되지 않으면 null 을 반환합니다.
 *
 * Timeout 전에 완료될 때:
 * ```
 * val result = withTimeoutOrNull(1000) {
 *     Thread.sleep(100)
 *     42
 * }
 * // result is 42
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```
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
    return runCatching {
        asyncRunWithTimeout(timeoutMillis, action = action).get()
    }.getOrNull()
}

/**
 * Timeout 내에서 [action]을 실행합니다. [action]이 [timeout] 시간 내에 종료되지 않으면 null 을 반환합니다.
 *
 * Timeout 전에 완료될 때:
 * ```
 * val result = withTimeoutOrNull(1000.milliseconds) {
 *     Thread.sleep(100)
 *     42
 * }
 * // result is 42
 * ```
 *
 * Timeout 이 걸릴 때:
 * ```
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
inline fun <T: Any> withTimeoutOrNull(timeout: Duration, crossinline action: () -> T): T? {
    return withTimeoutOrNull(timeout.inWholeMilliseconds, action)
}
