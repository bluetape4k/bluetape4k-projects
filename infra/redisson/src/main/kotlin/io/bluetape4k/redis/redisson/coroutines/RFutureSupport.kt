package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.concurrent.sequence
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.await
import org.redisson.api.RFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * [RFuture] 컬렉션을 하나의 [CompletableFuture]로 합칩니다.
 *
 * 모든 [RFuture]가 완료될 때까지 기다린 후 결과를 [List]로 반환하는 [CompletableFuture]를 생성합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val rfutures: List<RFuture<User>> = ids.map { rmap.getAsync(it) }
 * val future: CompletableFuture<List<User>> = rfutures.sequence()
 * val users: List<User> = future.get()
 * ```
 *
 * @param V 결과 타입
 * @param executor 비동기 처리에 사용할 [Executor] (기본값: [VirtualThreadExecutor])
 * @return 모든 결과를 담은 [List]를 반환하는 [CompletableFuture]
 */
fun <V> Iterable<RFuture<out V>>.sequence(
    executor: Executor = VirtualThreadExecutor,
): CompletableFuture<List<V>> = map { it.toCompletableFuture() }.sequence(executor)


/**
 * [RFuture] 컬렉션의 모든 결과를 코루틴에서 기다린 후 [List]로 반환합니다.
 *
 * 현재 코루틴 디스패처를 Executor로 활용하며, 디스패처가 없으면 [Dispatchers.Default]를 사용합니다.
 * 컬렉션이 비어 있으면 빈 리스트를 즉시 반환합니다.
 *
 * ## 사용 예
 * ```kotlin
 * runBlocking {
 *     val rfutures: List<RFuture<User>> = ids.map { rmap.getAsync(it) }
 *     val users: List<User> = rfutures.awaitAll()
 * }
 * ```
 *
 * @param V 결과 타입
 * @return 모든 [RFuture] 결과를 담은 [List]
 */
suspend fun <V> Collection<RFuture<out V>>.awaitAll(): List<V> {
    if (isEmpty()) {
        return emptyList()
    }

    val executor = currentCoroutineContext()[CoroutineDispatcher]?.asExecutor()
        ?: Dispatchers.Default.asExecutor()

    return sequence(executor).await()
}
