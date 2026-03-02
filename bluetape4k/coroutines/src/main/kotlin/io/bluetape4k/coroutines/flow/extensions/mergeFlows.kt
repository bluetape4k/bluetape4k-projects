package io.bluetape4k.coroutines.flow.extensions

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 여러 Flow를 동시에 수집해 도착 순서대로 병합합니다.
 *
 * ## 동작/계약
 * - 각 source를 별도 코루틴에서 수집하고, 도착한 값을 공용 큐에 넣어 하류로 방출합니다.
 * - source 간 상대 순서는 보장하지 않지만, 개별 source 내부 순서는 유지됩니다.
 * - 모든 source가 끝나고 큐가 비면 종료됩니다.
 * - 내부 큐(`ConcurrentLinkedQueue`)와 동기화용 `Resumable`을 사용합니다.
 *
 * ```kotlin
 * val result = listOf(flowOf(1, 3), flowOf(2, 4)).merge().toList()
 * // result는 [1, 2, 3, 4] 또는 [1, 3, 2, 4] 등 도착 순서에 따름
 * ```
 */
fun <T: Any> Iterable<Flow<T>>.merge(): Flow<T> = mergeInternal(this.toList())

/**
 * 여러 Flow를 동시에 수집해 도착 순서대로 병합합니다.
 *
 * ## 동작/계약
 * - vararg 입력을 리스트로 변환한 뒤 [mergeInternal]에 위임합니다.
 * - 입력이 비어 있으면 아무 값도 방출하지 않고 완료됩니다.
 *
 * @param sources 병합할 source Flow 목록입니다.
 */
fun <T: Any> merge(vararg sources: Flow<T>): Flow<T> = mergeInternal(sources.asList())

internal fun <T: Any> mergeInternal(sources: List<Flow<T>>): Flow<T> = flow {
    val queue = ConcurrentLinkedQueue<T>()
    val state = MergeState(sources.size)
    val ready = Resumable()

    coroutineScope {
        sources.forEach { source ->
            launch {
                try {
                    source.collect {
                        queue.offer(it)
                        ready.resume()
                    }
                } finally {
                    state.done.decrementAndGet()
                    ready.resume()
                }
            }
        }

        while (true) {
            val isDone = state.done.value == 0
            val value = queue.poll()

            when {
                isDone && value == null -> break
                value != null -> emit(value)
                else -> ready.await()
            }
        }
    }
}

private class MergeState(size: Int) {
    val done = atomic(size)
}
