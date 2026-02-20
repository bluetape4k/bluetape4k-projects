package io.bluetape4k.collections

import io.bluetape4k.support.assertInRange

/**
 * receiver 를 첫번째 요소로 하고, [tail]을 리스트의 후속 요소로 하는 리스트를 빌드합니다.
 *
 * ```
 * val list = 1 prependTo mutableListOf(2, 3)  // listOf(1, 2, 3)
 * ```
 *
 * @param tail 뒤에 붙을 컬렉션
 */
infix fun <T> T.prependTo(tail: MutableList<T>): MutableList<T> {
    tail.add(0, this)
    return tail
}

/**
 * [elements]를 리스트의 첫번째 요소로 추가한다
 *
 * @param elements 리스트 제일 앞에 추가할 요소들
 */
fun <T> MutableList<T>.prepend(vararg elements: T): MutableList<T> = apply {
    addAll(0, listOf(*elements))
}

/**
 * [elements]를 리스트의 끝에 추가한다
 *
 * @param elements 리스트 끝에 추가할 요소들
 */
fun <T> MutableList<T>.append(vararg elements: T): MutableList<T> = apply {
    addAll(elements)
    // plus(listOf(*elements))
}

/**
 * List 의 두 요소를 교환합니다.
 *
 * ```
 * val list = mutableListOf(1, 2, 3)
 * list.swap(0, 2)  // [3, 2, 1]
 * ```
 *
 * @param T
 * @param index1 첫 번째 인덱스
 * @param index2 두 번째 인덱스
 */
fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    if (index1 != index2) {
        index1.assertInRange(0, this.size - 1, "index1")
        index2.assertInRange(0, this.size - 1, "index2")

        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }
}

/**
 * 현 컬렉션의 항목 수가 지정한 항목 수보다 작다면, 새로운 컬렉션을 만들고, 기존 요소는 복사하고, `item` 값을 새롭게 할당된 공간에 추가합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val padded = list.padTo(5, 0)  // [1, 2, 3, 0, 0]
 * ```
 *
 * @param newSize 새로운 컬렉션의 크기
 * @param item 새롭게 추가될 요소의 값
 * @return 새로운 아이템이 추가된 컬렉션
 */
inline fun <reified T> List<T>.padTo(newSize: Int, item: T): List<T> {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    return this.toMutableList().apply {
        addAll(List(remains) { item })
    }
}

/**
 * 목록의 요소별로 카운트를 반환합니다.
 *
 * ```
 * val list = listOf(1,2,2,3)
 * val map = list.eachCount()  // {1=1, 2=2, 3=1}
 * ```
 *
 * @param T
 * @return
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
