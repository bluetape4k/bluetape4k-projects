package io.bluetape4k.coroutines.flow.extensions

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.toImmutableArray
import io.bluetape4k.support.requireGt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Flow 요소들을 sliding 방식으로 요소를 선택해서 제공합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val sliding = flow.immutableSliding(3)   // [1,2,3], [2,3,4], [3,4,5], [4,5], [5]
 * val sliding2 = flow.immutableSliding(3, false) // [1,2,3], [2,3,4], [3,4,5]
 * ```
 * @receiver Flow<T> 인스턴스
 * @param size sliding size. (require greater than 0)
 * @return Flow<ImmutableArray<T>> 인스턴스
 */
fun <T> Flow<T>.immutableSliding(size: Int, partialWindow: Boolean = true): Flow<ImmutableArray<T>> =
    immutableWindowed(size, 1, partialWindow)


/**
 * [size] 만큼이 채워지기 전까지는 현재 요소만 반환하고, 모든 요소가 채워지면, sliding으로 진행한다
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val sliding = flow.immutableBufferedSliding(3)   // [1], [1,2], [1,2,3], [2,3,4], [3,4,5], [4,5], [5]
 * val sliding2 = flow.immutableBufferedSliding(3, false)   // [1], [1,2], [1,2,3], [2,3,4], [3,4,5]
 * ```
 *
 * @param T
 * @param size sliding size. (require greater than 0)
 * @return Flow<List<T>> 인스턴스
 */
fun <T> Flow<T>.immutableBufferedSliding(size: Int, partialWindow: Boolean = true): Flow<ImmutableArray<T>> {
    size.requireGt(1, "size")

    return channelFlow {
        val queue = ArrayList<T>(size)

        this@immutableBufferedSliding.collect { elem ->
            if (queue.size >= size) {
                queue.removeFirst()
            }
            queue.add(elem)
            send(queue.toImmutableArray())
        }

        if (partialWindow) {
            while (queue.isNotEmpty()) {
                queue.removeFirst()
                if (queue.isNotEmpty()) {
                    send(queue.toImmutableArray())
                }
            }
        }
    }
}
