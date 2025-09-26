package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.concurrent.sequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import org.redisson.api.RFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

/**
 * [RFuture]의 컬렉션을 하나의 CompletableFuture로 변환합니다.
 *
 * ```
 * val rfutures: List<RFuture<User>> = ids.map { rmap.getAsync(it) }
 * val future: CompletableFuture<List<User>> = rfutures.sequence()
 * val users: List<User> = future.get()
 * ```
 */
fun <V> Iterable<RFuture<out V>>.sequence(
    executor: Executor = Dispatchers.IO.asExecutor(),
): CompletableFuture<List<V>> = map { it.toCompletableFuture() }.sequence(executor)


/**
 * [RFuture]의 컬렉션을 결과를 모두 기다리고 List 로 반환한다
 *
 * ```
 * runBlocking {
 *     val rfutures: List<RFuture<User>> = ids.map { rmap.getAsync(it) }
 *     val users: List<User> = rfutures.awaitAll()
 * }
 */
suspend fun <V> Collection<RFuture<out V>>.awaitAll(): List<V> {
    val executor = coroutineContext[CoroutineDispatcher]?.asExecutor()
        ?: Dispatchers.Default.asExecutor()

    return sequence(executor).await()
}
