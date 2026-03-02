package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireGt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 고정 크기/간격으로 이동하는 윈도우 리스트를 생성합니다.
 *
 * ## 동작/계약
 * - `size > 0`, `step > 0`, `size >= step`을 검증하며 위반 시 예외가 발생합니다.
 * - 윈도우가 가득 차면 방출 후 `step`만큼 앞부분을 버리고 다음 윈도우를 만듭니다.
 * - `partialWindow=true`면 마지막 불완전 윈도우도 추가 방출합니다.
 * - 윈도우마다 `ArrayList`를 생성해 방출합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3, 4).windowed(size = 3, step = 2, partialWindow = true).toList()
 * // result == [[1, 2, 3], [3, 4]]
 * ```
 *
 * @param size 윈도우 크기입니다. 0 이하면 예외가 발생합니다.
 * @param step 윈도우 이동 간격입니다. 0 이하면 예외가 발생하며 `size`보다 클 수 없습니다.
 * @param partialWindow 마지막 불완전 윈도우 방출 여부입니다.
 */
fun <T> Flow<T>.windowed(size: Int, step: Int = 1, partialWindow: Boolean = false): Flow<List<T>> =
    windowedInternal(size, step, partialWindow)

/**
 * [windowed] 결과를 `Flow<T>` 윈도우로 변환해 방출합니다.
 *
 * ## 동작/계약
 * - 윈도우 생성 규칙은 [windowed]와 동일합니다.
 * - 각 리스트 윈도우를 `asFlow()`로 감싼 새 Flow를 방출합니다.
 * - 윈도우 수만큼 추가 Flow 객체가 생성됩니다.
 *
 * ```kotlin
 * val windows = flowOf(1, 2, 3).windowedFlow(size = 2, step = 1, partialWindow = false)
 * // windows는 [1,2], [2,3] 두 개의 Flow를 방출
 * ```
 *
 * @param size 윈도우 크기입니다.
 * @param step 윈도우 이동 간격입니다.
 * @param partialWindow 마지막 불완전 윈도우 방출 여부입니다.
 */
fun <T> Flow<T>.windowedFlow(size: Int, step: Int, partialWindow: Boolean = false): Flow<Flow<T>> =
    windowedInternal(size, step, partialWindow).map { it.asFlow() }

private fun <T> Flow<T>.windowedInternal(
    size: Int,
    step: Int = 1,
    partialWindow: Boolean = false,
): Flow<List<T>> = flow {
    size.requireGt(0, "size")
    step.requireGt(0, "step")
    size.requireGe(step, "step")

    var elements: MutableList<T> = ArrayList(size)
    var counter = 0

    this@windowedInternal.collect { element ->
        elements.add(element)
        counter++
        if (counter == size) {
            emit(elements)
            val next = ArrayList<T>(size)
            for (i in step until elements.size) {
                next.add(elements[i])
            }
            elements = next
            counter -= step
        }
    }

    if (partialWindow) {
        while (counter > 0) {
            emit(elements)
            val next = ArrayList<T>(size)
            for (i in step until elements.size) {
                next.add(elements[i])
            }
            elements = next
            counter -= step
        }
    }
}
