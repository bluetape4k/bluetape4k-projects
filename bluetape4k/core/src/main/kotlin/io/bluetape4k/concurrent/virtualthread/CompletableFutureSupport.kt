package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.CompletableFuture

/**
 * 지정한 block을 Virtual Threads 를 이용하여 비동기로 실행하고, [CompletableFuture]를 반환합니다.
 *
 * ```kotlin
 * val future: CompletableFuture<Int> = virtualFutureOf {
 *      Thread.sleep(1000)
 *      42
 * }
 * val result = future.get() // 42
 * ```
 *
 * @param V 작업 결과 타입
 * @param block 비동기로 수행할 작업
 * @return Virtual Thread 위에서 [block]을 실행하는 [CompletableFuture] 인스턴스
 */
inline fun <V: Any> virtualFutureOf(
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)

/**
 * 지정한 block을 Virtual Threads 를 이용하여 비동기로 실행하고, nullable 결과를 허용하는 [CompletableFuture]를 반환합니다.
 *
 * ```kotlin
 * val future: CompletableFuture<Int?> = virtualFutureOfNullable {
 *      Thread.sleep(1000)
 *      null // 또는 42
 * }
 * val result = future.get() // null 또는 42
 * ```
 *
 * @param V 작업 결과 타입 (nullable 허용)
 * @param block 비동기로 수행할 작업
 * @return Virtual Thread 위에서 [block]을 실행하는 [CompletableFuture] 인스턴스
 */
inline fun <V> virtualFutureOfNullable(
    crossinline block: () -> V?,
): CompletableFuture<V?> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
