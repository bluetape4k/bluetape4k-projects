package io.bluetape4k.coroutines.flow.extensions

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.toImmutableArray
import io.bluetape4k.collections.eclipse.asFastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.support.requireGt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Flow 요소들을 windowing 을 수행하여 `Flow<ImmutableArray<T>>` 로 변환합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val windowed = flow.immutableWindowed(3, 1, true)   // [1,2,3], [2,3,4], [3,4,5], [4,5], [5]
 * val windowed2 = flow.immutableWindowed(3, 1, false)   // [1,2,3], [2,3,4], [3,4,5]
 * ```
 *
 * @param size window size (require positive number)
 * @param step step (require positive number) (default 1)
 * @return `Flow<ImmutableArray<T>>` 인스턴스
 *
 * @see [io.bluetape4k.coroutines.flow.extensions.sliding]
 * @see [io.bluetape4k.coroutines.flow.extensions.chunked]
 */
fun <T> Flow<T>.immutableWindowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): Flow<ImmutableArray<T>> =
    immutableWindowedInternal(size, step, partialWindows)

private fun <T> Flow<T>.immutableWindowedInternal(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): Flow<ImmutableArray<T>> = channelFlow {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var elements = fastListOf<T>()
    val counter = AtomicInteger(0)

    this@immutableWindowedInternal.collect { elem ->
        elements.add(elem)
        if (counter.incrementAndGet() == size) {
            send(elements.toImmutableArray())
            elements = elements.drop(step).asFastList()
            counter.addAndGet(-step)
        }
    }
    if (partialWindows) {
        while (counter.get() > 0) {
            send(elements.toImmutableArray())
            elements = elements.drop(step).asFastList()
            counter.addAndGet(-step)
        }
    }
}
