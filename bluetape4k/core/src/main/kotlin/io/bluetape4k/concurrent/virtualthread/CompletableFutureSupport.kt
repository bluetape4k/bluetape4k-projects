package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.CompletableFuture

/**
 * Runs [block] asynchronously on the shared virtual-thread executor and returns a [CompletableFuture].
 *
 * ```kotlin
 * val future: CompletableFuture<Int> = virtualFutureOf {
 *      Thread.sleep(1000)
 *      42
 * }
 * val result = future.get() // 42
 * ```
 *
 * @param V result type.
 * @param block work to run on a virtual thread.
 * @return a [CompletableFuture] completed with [block]'s result.
 */
inline fun <V: Any> virtualFutureOf(
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)

/**
 * Runs [block] asynchronously on the shared virtual-thread executor and returns a nullable [CompletableFuture].
 *
 * ```kotlin
 * val future: CompletableFuture<Int?> = virtualFutureOfNullable {
 *      Thread.sleep(1000)
 *      null // 또는 42
 * }
 * val result = future.get() // null 또는 42
 * ```
 *
 * @param V result type.
 * @param block work to run on a virtual thread.
 * @return a [CompletableFuture] completed with [block]'s nullable result.
 */
inline fun <V> virtualFutureOfNullable(
    crossinline block: () -> V?,
): CompletableFuture<V?> =
    CompletableFuture.supplyAsync({ block() }, VirtualThreadExecutor)
