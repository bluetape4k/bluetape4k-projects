package io.bluetape4k.support

import io.bluetape4k.concurrent.get
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
 * @param timeoutMillis 제한 시간
 * @param action 비동기로 실행할 코드 블럭
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면 [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
 */
fun <T> asyncRunWithTimeout(
    timeoutMillis: Long,
    action: () -> T,
): CompletableFuture<T> {
    val executor = Executors.newVirtualThreadPerTaskExecutor()
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
 * @return [action]의 실행 결과를 담은 [CompletableFuture], 제한시간이 초과되면 [java.util.concurrent.TimeoutException]을 담은 [CompletableFuture]를 반환합니다.
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
fun <T: Any> withTimeoutOrNull(timeoutMillis: Long, action: () -> T): T? {
    return try {
        asyncRunWithTimeout(timeoutMillis, action = action).get(timeoutMillis.milliseconds)
    } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause is TimeoutException) null else throw e
    } catch (e: TimeoutException) {
        null
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
fun <T: Any> withTimeoutOrNull(timeout: Duration, action: () -> T): T? =
    withTimeoutOrNull(timeout.inWholeMilliseconds, action)

fun <T: Any> retryWithTimeoutOrNull(
    maxRetries: Int,
    timeout: Duration,
    action: () -> T,
): T? {
    maxRetries.requirePositiveNumber("maxRetries")

    repeat(maxRetries) { attempt ->
        val result = withTimeoutOrNull(timeout, action)
        if (result != null) return result
        // log.debug { "시도 ${attempt + 1}/$maxRetries 타임아웃" }
    }
    return null
}
