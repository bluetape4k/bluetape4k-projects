package io.bluetape4k.redis.lettuce

import io.bluetape4k.concurrent.sequence
import io.lettuce.core.RedisFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

/**
 * Awaits for completions of [RedisFuture] without blocking a thread.
 */
@Deprecated("Use coAwait instead", ReplaceWith("coAwait()"))
suspend fun <T> RedisFuture<T>.await(): T {
    return asDeferred().await()
}

/**
 * Awaits for completions of [RedisFuture] without blocking a thread.
 *
 * ```
 * val result = redisAsyncCommands.get("key").coAwait()
 * ```
 */
suspend fun <T> RedisFuture<T>.coAwait(): T {
    return await()
}

/**
 * [RedisFuture]`<T>` 컬렉션의 모든 요소들이 완료될 때까지 대기합니다.
 *
 * ```
 * val results = listOf(
 *      redisAsyncCommands.get("key1"),
 *      redisAsyncCommands.get("key2")
 * ).awaitAll()
 * ```
 */
suspend fun <T> Collection<RedisFuture<out T>>.awaitAll(): List<T> {
    return when {
        this.isEmpty() -> emptyList()
        else           -> sequence().await()
    }
}

/**
 * [RedisFuture]`<T>` 컬렉션의 모든 요소들이 [CompletableFuture]`<List<T>>`로 변환합니다.
 *
 * ```
 * val future: CompletableFuture<List<T>> = listOf(
 *      redisAsyncCommands.get("key1"),
 *      redisAsyncCommands.get("key2")
 * ).sequence()
 *
 * val results = future.get()
 * ```
 */
fun <T> Iterable<RedisFuture<out T>>.sequence(
    executor: Executor = ForkJoinPool.commonPool(),
): CompletableFuture<List<T>> {
    return map { it.toCompletableFuture() }.sequence(executor)
}
