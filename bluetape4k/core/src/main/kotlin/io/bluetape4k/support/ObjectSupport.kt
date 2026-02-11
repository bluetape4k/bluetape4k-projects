package io.bluetape4k.support

/**
 * [items] 중에 가장 중복이 많은 항목을 찾습니다. 없으면 null을 반환합니다.
 *
 * ```
 * val mode = modeOrNull(1, 2, 2, 3, 4, 4, 4, 5) // 4
 * modeOrNull(1, 1, 2, 2, 3, 3) // null
 * modeOrNull(1, 2, 3, 4, 5) // null
 * ```
 *
 * @param items 중복을 찾을 항목들
 * @return 중복이 가장 많은 항목
 */
fun <T: Any> modeOrNull(vararg items: T): T? = items.asIterable().modeOrNull()

/**
 * [Sequence]의 요소 중 가장 중복이 많은 항목을 찾습니다. 없으면 null을 반환합니다.
 *
 * ```
 * val mode = sequenceOf(1, 2, 2, 3, 4, 4, 4, 5).modeOrNull() // 4
 * sequenceOf(1, 2, 2, 3, 3, 4, 5).modeOrNull() // null
 * sequenceOf(1, 2, 3, 4, 5).modeOrNull() // null
 * ```
 */
fun <T: Any> Sequence<T>.modeOrNull(): T? = asIterable().modeOrNull()

/**
 * [Iterable]의 요소 중 가장 중복이 많은 항목을 찾습니다. 없으면 null을 반환합니다.
 *
 * ```
 * val mode = listOf(1, 2, 2, 3, 4, 4, 4, 5).modeOrNull() // 4
 * listOf(1, 2, 2, 3, 3, 4, 5).modeOrNull() // null
 * listOf(1, 2, 3, 4, 5).modeOrNull() // null
 * ```
 */
fun <T: Any> Iterable<T>.modeOrNull(): T? {
    val counts = groupingBy { it }.eachCount()
    val maxCount = counts.values.maxOrNull() ?: return null
    val maxEntries = counts.entries.filter { it.value == maxCount }
    return if (maxEntries.size == 1) maxEntries.first().key else null
}

/**
 * [Collection]의 중간 값을 찾습니다. 값이 짝수개일 경우 두 중간 값 중 작은 값을 반환합니다.
 *
 * ```
 * listOf(1, 2, 3, 4, 5).median() // 3
 * listOf(1, 1, 1).median() // 1
 * listOf(1, 2, 3, 4).median() // 2
 * listOf(1, 1, 1, 2, 3).median() // 1
 * ```
 *
 * @receiver 처리할 값들의 컬렉션
 * @param <T> 이 메서드에서 처리하는 값의 타입
 * @return T 중간 값
 */
fun <T: Comparable<T>> Collection<T>.median(): T {
    this.requireNotEmpty("median")
    val sorted = sorted()
    return sorted[(sorted.size - 1) / 2]
}

/**
 * [Collection]의 중간 값을 찾습니다. 값이 짝수개일 경우 두 중간 값 중 작은 값을 반환합니다.
 *
 * ```
 * val comparator = Comparator<Int> { o1, o2 -> o2 - o1 }
 * listOf(1, 2, 3, 4, 5).median(comparator) // 3
 * listOf(1, 1, 1).median(comparator) // 1
 * listOf(1, 2, 3, 4).median(comparator) // 3
 * ```
 *
 * @receiver 처리할 값들의 컬렉션
 * @param comparator 값 비교를 위한 Comparator
 * @param <T> 이 메서드에서 처리하는 값의 타입
 * @return T 중간 값
 */
fun <T> Collection<T>.median(comparator: Comparator<T>): T {
    this.requireNotEmpty("median")
    val sorted = sortedWith(comparator)
    return sorted[(sorted.size - 1) / 2]
}
