package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 인접한 요소를 같은 그룹 키 구간 단위로 묶어 방출합니다.
 *
 * ## 동작/계약
 * - 연속 구간의 `groupSelector` 결과가 같으면 같은 버퍼에 누적하고, 키가 바뀌는 시점에 이전 버퍼를 방출합니다.
 * - 빈 upstream이면 아무 것도 방출하지 않습니다.
 * - 마지막 누적 버퍼는 collect 종료 시 1회 방출됩니다.
 * - 구간 전환마다 `toList()` 복사본을 만들어 방출합니다.
 *
 * ```kotlin
 * val result = mutableListOf<List<Int>>()
 * flowOf(1, 1, 2, 3, 3).bufferUntilChanged { it }.collect { result += it }
 * // result == [[1, 1], [2], [3, 3]]
 * ```
 *
 * @param groupSelector 요소의 그룹 키를 계산하는 함수입니다.
 */
fun <T, K> Flow<T>.bufferUntilChanged(groupSelector: (T) -> K): Flow<List<T>> = flow {
    val self = this@bufferUntilChanged
    val elements = mutableListOf<T>()
    var prevGroup: K? = null

    self.collect { element ->
        val currentGroup = groupSelector(element)
        prevGroup = prevGroup ?: currentGroup

        if (prevGroup == currentGroup) {
            elements.add(element)
        } else {
            emit(elements.toList())
            elements.clear()
            elements.add(element)
            prevGroup = currentGroup
        }
    }

    if (elements.isNotEmpty()) {
        emit(elements.toList())
        elements.clear()
    }
}
