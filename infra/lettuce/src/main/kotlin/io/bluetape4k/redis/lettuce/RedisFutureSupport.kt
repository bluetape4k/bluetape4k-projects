package io.bluetape4k.redis.lettuce

import io.bluetape4k.concurrent.sequence
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.lettuce.core.RedisFuture
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * [RedisFuture] 완료를 스레드 블로킹 없이 suspend 방식으로 대기합니다.
 *
 * Lettuce async API를 코루틴 코드에서 사용할 때 `get()` 대신 사용합니다. 대기 중인 코루틴이
 * 취소되면 `kotlinx.coroutines.future.await`의 취소 전파 규칙을 따릅니다.
 *
 * ```kotlin
 * val value: String? = redisAsyncCommands.get("user:1").awaitSuspending()
 * ```
 */
suspend inline fun <T> RedisFuture<T>.awaitSuspending(): T = await()

/**
 * [RedisFuture] 완료를 스레드 블로킹 없이 suspend 방식으로 대기합니다.
 *
 * @see awaitSuspending
 *
 * ```kotlin
 * val value: String? = redisAsyncCommands.get("user:1").suspendAwait()
 * ```
 */
@Deprecated(
    message = "Use awaitSuspending() instead.",
    replaceWith = ReplaceWith("awaitSuspending()"),
)
suspend inline fun <T> RedisFuture<T>.suspendAwait(): T = await()


/**
 * [RedisFuture] 완료를 스레드 블로킹 없이 suspend 방식으로 대기합니다.
 *
 * @see awaitSuspending
 *
 * ```kotlin
 * val value: String? = redisAsyncCommands.get("user:1").coAwait()
 * ```
 */
@Deprecated(
    message = "Use awaitSuspending() instead.",
    replaceWith = ReplaceWith("awaitSuspending()"),
)
suspend inline fun <T> RedisFuture<T>.coAwait(): T = await()

/**
 * [RedisFuture] 컬렉션의 모든 요소가 완료될 때까지 스레드 블로킹 없이 대기합니다.
 *
 * 입력 순서대로 결과를 반환합니다. 하나라도 실패하면 반환 [List]를 만들지 않고 해당 예외가 전파됩니다.
 * 빈 컬렉션은 즉시 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val results = listOf(
 *     redisAsyncCommands.get("user:1"),
 *     redisAsyncCommands.get("user:2"),
 * ).awaitAll()
 * ```
 */
suspend inline fun <T> Collection<RedisFuture<out T>>.awaitAll(): List<T> = when {
    this.isEmpty() -> emptyList()
    else -> sequence().await()
}

/**
 * [RedisFuture] 컬렉션을 입력 순서를 보존하는 [CompletableFuture]`<List<T>>`로 변환합니다.
 *
 * 모든 future가 완료된 뒤 단일 continuation에서 결과 리스트를 만듭니다. 대량 명령에서는 future마다
 * 별도 코루틴을 만들기보다 [awaitAll] 또는 이 함수를 사용하는 편이 가볍습니다.
 *
 * ```kotlin
 * val future: CompletableFuture<List<T>> = listOf(
 *     redisAsyncCommands.get("user:1"),
 *     redisAsyncCommands.get("user:2"),
 * ).sequence()
 *
 * val results: List<T> = future.get()
 * ```
 */
fun <T> Iterable<RedisFuture<out T>>.sequence(
    executor: Executor = VirtualThreadExecutor, // Dispatchers.IO.asExecutor(),
): CompletableFuture<List<T>> = map { it.toCompletableFuture() }.sequence(executor)
