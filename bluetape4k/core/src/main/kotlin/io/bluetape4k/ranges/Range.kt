package io.bluetape4k.ranges

/**
 * 값의 범위를 나타내는 인터페이스입니다.
 *
 * 네 가지 경계 조합을 지원합니다:
 * - [ClosedClosedRange]: `[first, last]` (양쪽 포함)
 * - [ClosedOpenRange]: `[first, last)` (하한 포함, 상한 미포함)
 * - [OpenClosedRange]: `(first, last]` (하한 미포함, 상한 포함)
 * - [OpenOpenRange]: `(first, last)` (양쪽 미포함)
 */
interface Range<T: Comparable<T>> {

    /**
     * 범위의 하한 값
     */
    val first: T

    /**
     * 범위의 상한 값
     */
    val last: T

    /**
     * 하한이 포함되는지 여부 (Closed이면 `true`, Open이면 `false`)
     */
    val isStartInclusive: Boolean get() = true

    /**
     * 상한이 포함되는지 여부 (Closed이면 `true`, Open이면 `false`)
     */
    val isEndInclusive: Boolean get() = true

    operator fun contains(value: T): Boolean

    fun isEmpty(): Boolean
}

/**
 * 이 범위가 다른 범위 [other]를 완전히 포함하는지 확인합니다.
 * 경계 타입(Open/Closed)을 고려하여 정확하게 판단합니다.
 *
 * 예: `[0, 10].contains((2, 8))` → `true`, `(0, 10).contains([0, 10])` → `false`
 */
fun <T: Comparable<T>> Range<T>.contains(other: Range<T>): Boolean {
    // 이 범위의 하한이 Open이고 other의 하한이 Closed이면 strict 비교 필요
    val lowerOk = if (!isStartInclusive && other.isStartInclusive) {
        first < other.first
    } else {
        first <= other.first
    }
    // 이 범위의 상한이 Open이고 other의 상한이 Closed이면 strict 비교 필요
    val upperOk = if (!isEndInclusive && other.isEndInclusive) {
        last > other.last
    } else {
        last >= other.last
    }
    return lowerOk && upperOk
}

/**
 * 두 범위가 겹치는(공통 원소가 존재하는)지 확인합니다.
 *
 * 예: `[0, 5].overlaps([5, 10])` → `true`, `[0, 5).overlaps([5, 10])` → `false`
 */
fun <T: Comparable<T>> Range<T>.overlaps(other: Range<T>): Boolean {
    // 이 범위의 상한과 other의 하한이 만나는 지점
    val upperVsLower = if (isEndInclusive && other.isStartInclusive) {
        last >= other.first
    } else {
        last > other.first
    }
    // other의 상한과 이 범위의 하한이 만나는 지점
    val otherUpperVsLower = if (other.isEndInclusive && isStartInclusive) {
        other.last >= first
    } else {
        other.last > first
    }
    return upperVsLower && otherUpperVsLower
}

/**
 * [Range]의 컬렉션이 [first] 값 기준으로 오름차순 정렬되어 있는지 확인합니다.
 */
fun <T: Comparable<T>> Iterable<Range<T>>.isAscending(): Boolean {
    val first = firstOrNull() ?: return true
    var max = first.first
    return drop(1).fold(true) { isAscending, range ->
        val newAscending = isAscending && (max <= range.first)
        max = maxOf(max, range.first)
        newAscending
    }
}