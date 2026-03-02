package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireGt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 한 칸씩 이동하는 슬라이딩 윈도우를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `windowed(size, step = 1, partialWindow)`에 위임합니다.
 * - `size` 검증/예외 규칙은 `windowed` 구현을 따릅니다.
 * - 각 윈도우는 `List`로 할당되어 방출됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).sliding(2).toList()
 * // result == [[1, 2], [2, 3], [3]]
 * ```
 *
 * @param size 윈도우 크기입니다.
 * @param partialWindow 마지막 불완전 윈도우 방출 여부입니다.
 */
fun <T> Flow<T>.sliding(size: Int, partialWindow: Boolean = true): Flow<List<T>> =
    windowed(size, 1, partialWindow)

/**
 * 고정 크기 버퍼를 유지하며 매 요소마다 현재 버퍼 스냅샷을 방출합니다.
 *
 * ## 동작/계약
 * - `size.requireGt(1, "size")`를 검증하며 1 이하면 예외가 발생합니다.
 * - 버퍼가 가득 차면 가장 오래된 요소를 제거하고 새 요소를 추가합니다.
 * - 수집 종료 후 버퍼를 한 칸씩 비우며 남은 창을 추가 방출합니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3).bufferedSliding(2).toList()
 * // result == [[1], [1, 2], [2, 3], [3]]
 * ```
 *
 * @param size 내부 버퍼 크기입니다. 1 이하면 예외가 발생합니다.
 */
fun <T> Flow<T>.bufferedSliding(size: Int): Flow<List<T>> = flow {
    size.requireGt(1, "size")
    val queue = ArrayList<T>(size)

    this@bufferedSliding.collect { element ->
        if (queue.size >= size) {
            queue.removeFirst()
        }
        queue.add(element)
        emit(queue.toList())
    }

    while (queue.isNotEmpty()) {
        queue.removeFirst()
        if (queue.isNotEmpty()) {
            emit(queue.toList())
        }
    }
}
