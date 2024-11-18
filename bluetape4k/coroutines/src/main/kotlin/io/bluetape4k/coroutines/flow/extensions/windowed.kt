package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireGt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map

/**
 * Flow 요소들을 windowing 을 수행하여 `Flow<List<T>>` 로 변환합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val windowed = flow.windowed(3, 1)   // {1,2,3}, {2,3,4}, {3,4,5}, {4,5}, {5}
 * ```
 *
 * @param size window size (require positive number)
 * @param step step (require positive number)
 * @return `Flow<List<T>>` 인스턴스
 *
 * @see [io.bluetape4k.coroutines.flow.extensions.sliding]
 * @see [io.bluetape4k.coroutines.flow.extensions.chunked]
 */
fun <T> Flow<T>.windowed(size: Int, step: Int): Flow<List<T>> =
    windowedInternal(size, step)

/**
 * Flow 요소들을 windowing 을 수행하여 `Flow<Flow<T>>` 로 변환합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val windowed = flow.windowedFlow(3, 1)   // {1,2,3}, {2,3,4}, {3,4,5}, {4,5}, {5}
 * ```
 * @param size window size (require positive number)
 * @param step step (require positive number)
 * @return `Flow<Flow<T>>` 인스턴스
 *
 * @see [windowed]
 */
fun <T> Flow<T>.windowedFlow(size: Int, step: Int): Flow<Flow<T>> =
    windowedInternal(size, step).map { it.asFlow() }

private fun <T> Flow<T>.windowedInternal(size: Int, step: Int): Flow<List<T>> = channelFlow {
    size.requireGt(0, "size")
    step.requireGt(0, "step")
    size.requireGe(step, "step")

    var elements: MutableList<T> = ArrayList(size)
    val counter = atomic(0)

    this@windowedInternal.collect { element ->
        elements.add(element)
        if (counter.incrementAndGet() == size) {
            send(elements)
            elements = elements.drop(step).toMutableList()
            counter.addAndGet(-step)
        }
    }

    while (counter.value > 0) {
        send(elements.take(step))
        elements = elements.drop(step).toMutableList()
        counter.addAndGet(-step)
    }
}
