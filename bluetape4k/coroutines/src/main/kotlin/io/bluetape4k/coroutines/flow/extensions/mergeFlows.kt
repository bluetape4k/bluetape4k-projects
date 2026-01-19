package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.collections.eclipse.toFastList
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 여러 Flow 소스를 무제한으로 병합합니다.
 *
 * ```
 * listOf(
 *     flowRangeOf(6, 5).log(6),
 *     flowRangeOf(1, 5).log(1),
 * )
 *     .merge()
 *     .assertResultSet(6, 7, 8, 9, 10, 1, 2, 3, 4, 5)
 * ```
 */
fun <T: Any> Iterable<Flow<T>>.merge(): Flow<T> = mergeInternal(this.toFastList())

/**
 * 여러 Flow 소스를 무제한으로 병합합니다.
 *
 * ```
 * merge(
 *     flowRangeOf(6, 5).log(6),
 *     flowRangeOf(1, 5).log(1),
 * )
 *     .assertResultSet(6, 7, 8, 9, 10, 1, 2, 3, 4, 5)
 * ```
 */
fun <T: Any> merge(vararg sources: Flow<T>): Flow<T> = mergeInternal(sources.toFastList())

/**
 * 여러 Flow 소스를 무제한으로 병합합니다.
 */
internal fun <T: Any> mergeInternal(sources: List<Flow<T>>): Flow<T> = channelFlow {
    val queue = ConcurrentLinkedQueue<T>()
    val done = atomic(sources.size)
    val ready = Resumable()

    coroutineScope {
        // 모든 source 로부터 요소를 받아 queue 에 저장한다.
        sources.forEach { source ->
            launch {
                try {
                    source.collect {
                        queue.offer(it)
                        ready.resume()
                    }
                } finally {
                    done.decrementAndGet()
                    ready.resume()
                }
            }
        }

        while (true) {
            val isDone = done.value == 0
            val value = queue.poll()

            when {
                isDone && value == null -> break
                value != null -> send(value)
                else -> ready.await()
            }
        }
    }
}
