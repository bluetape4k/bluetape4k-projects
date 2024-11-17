package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireGt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Flow 요소들을 sliding 방식으로 요소를 선택해서 제공합니다.
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val sliding = flow.sliding(3)   // {1,2,3}, {2,3,4}, {3,4,5}, {4,5}, {5}
 * ```
 * @receiver Flow<T> 인스턴스
 * @param size sliding size. (require greater than 0)
 * @return Flow<List<T>> 인스턴스
 */
fun <T> Flow<T>.sliding(size: Int): Flow<List<T>> = windowed(size, 1)

/**
 * [size] 만큼이 채워지기 전까지는 현재 요소만 반환하고, 모든 요소가 채워지면, sliding으로 진행한다
 *
 * ```
 * val flow = flowOf(1,2,3,4,5)
 * val sliding = flow.bufferedSliding(3)   // {1}, {1,2}, {1,2,3}, {2,3,4}, {3,4,5}
 * ```
 *
 * @param T
 * @param size sliding size. (require greater than 0)
 * @return Flow<List<T>> 인스턴스
 */
fun <T> Flow<T>.bufferedSliding(size: Int): Flow<List<T>> = channelFlow {
    size.requireGt(1, "sliding size")
    val queue = ArrayList<T>(size)

    this@bufferedSliding.collect { element ->
        if (queue.size >= size) {
            queue.removeFirst()
        }
        queue.add(element)
        send(queue.toList())
    }

    while (queue.isNotEmpty()) {
        queue.removeFirst()
        if (queue.isNotEmpty()) {
            send(queue.toList())
        }
    }
}
