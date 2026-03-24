package io.bluetape4k.collections

import io.bluetape4k.support.assertInRange

/**
 * 수신 객체(this)를 리스트의 **첫 번째 요소**로 추가하고, [tail]을 그대로 반환합니다.
 *
 * ## 동작/계약
 * - [tail]을 **직접 변경(mutate)** 합니다.
 * - 시간 복잡도는 `O(n)` 입니다(앞에 삽입하므로 요소 이동이 발생).
 *
 * @param tail 뒤에 붙을 리스트(변경됨)
 * @return 변경된 [tail]
 *
 * ```kotlin
 * val result = 1 prependTo mutableListOf(2, 3)
 * // result == [1, 2, 3]
 * ```
 */
infix fun <T> T.prependTo(tail: MutableList<T>): MutableList<T> {
    tail.add(0, this)
    return tail
}

/**
 * [elements]를 리스트의 **맨 앞**에 추가하고, 리스트 자신을 반환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트를 **직접 변경(mutate)** 합니다.
 * - 요소를 앞에 붙이므로 일반적으로 `O(n + m)` 입니다(`n`=기존 크기, `m`=추가 요소 수).
 *
 * @param elements 리스트 제일 앞에 추가할 요소들
 * @return 변경된 수신 리스트
 *
 * ```kotlin
 * val result = mutableListOf(3, 4).prepend(1, 2)
 * // result == [1, 2, 3, 4]
 * ```
 */
fun <T> MutableList<T>.prepend(vararg elements: T): MutableList<T> = apply {
    addAll(0, listOf(*elements))
}

/**
 * [elements]를 리스트의 **맨 뒤**에 추가하고, 리스트 자신을 반환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트를 **직접 변경(mutate)** 합니다.
 * - `ArrayList` 기준으로 평균 `O(m)` 입니다(용량 확장 시 재할당이 발생할 수 있음).
 *
 * @param elements 리스트 끝에 추가할 요소들
 * @return 변경된 수신 리스트
 *
 * ```kotlin
 * val result = mutableListOf(1, 2).append(3, 4)
 * // result == [1, 2, 3, 4]
 * ```
 */
fun <T> MutableList<T>.append(vararg elements: T): MutableList<T> = apply {
    addAll(elements)
    // plus(listOf(*elements))
}

/**
 * 리스트에서 [index1]과 [index2] 위치의 요소를 교환합니다.
 *
 * ## 동작/계약
 * - 두 인덱스가 같으면 아무 작업도 하지 않습니다.
 * - 인덱스 범위는 `0..lastIndex`여야 하며, 범위를 벗어나면 [assertInRange]에 의해 예외가 발생합니다.
 * - 수신 리스트를 **직접 변경(mutate)** 합니다.
 *
 * @param index1 첫 번째 인덱스
 * @param index2 두 번째 인덱스
 *
 * ```kotlin
 * val values = mutableListOf(1, 2, 3)
 * values.swap(0, 2)
 * // values == [3, 2, 1]
 * ```
 */
fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    if (index1 == index2) return

    index1.assertInRange(0, this.size - 1, "index1")
    index2.assertInRange(0, this.size - 1, "index2")

    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

/**
 * 리스트의 크기가 [newSize]보다 작으면, 부족한 만큼 [item]을 뒤에 채워 **새 리스트**를 반환합니다.
 *
 * ## 동작/계약
 * - `newSize <= size`이면 **동일한 리스트(this)** 를 그대로 반환합니다(할당 없음).
 * - `newSize > size`이면 새 `MutableList`를 생성하고 뒤에 [item]을 채웁니다.
 *
 * @param newSize 목표 크기
 * @param item 채울 값
 * @return 패딩된 리스트(조건에 따라 this 또는 새 리스트)
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).padTo(5, 0)
 * // result == [1, 2, 3, 0, 0]
 * ```
 */
fun <T> List<T>.padTo(newSize: Int, item: T): List<T> {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    return this.toMutableList().apply {
        addAll(List(remains) { item })
    }
}

/**
 * 리스트의 각 요소별 등장 횟수를 계산하여 반환합니다.
 *
 * ## 동작/계약
 * - 빈 리스트면 `emptyMap()`을 반환합니다.
 * - 결과는 입력 순서를 유지하는 [LinkedHashMap] 기반입니다.
 *
 * @return 요소 -> 등장 횟수
 *
 * ```kotlin
 * val result = listOf(1, 2, 2, 3).eachCount()
 * // result == {1=1, 2=2, 3=1}
 * ```
 */
fun <T> List<T>.eachCount(): Map<T, Int> {
    if (isEmpty()) {
        return emptyMap()
    }

    val counts = LinkedHashMap<T, Int>(this.size)
    for (value in this) {
        counts[value] = (counts[value] ?: 0) + 1
    }
    return counts
}
