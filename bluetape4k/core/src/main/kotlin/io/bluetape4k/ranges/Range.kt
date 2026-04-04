package io.bluetape4k.ranges

/**
 * 값의 범위를 나타내는 인터페이스입니다.
 *
 * 네 가지 경계 조합을 지원합니다:
 * - [ClosedClosedRange]: `[first, last]` (양쪽 포함)
 * - [ClosedOpenRange]: `[first, last)` (하한 포함, 상한 미포함)
 * - [OpenClosedRange]: `(first, last]` (하한 미포함, 상한 포함)
 * - [OpenOpenRange]: `(first, last)` (양쪽 미포함)
 *
 * ### 예제: 4가지 Range 타입 생성 및 경계 비교
 * ```kotlin
 * val closed = closedClosedRangeOf(0, 10)   // [0, 10]
 * val closedOpen = closedOpenRangeOf(0, 10) // [0, 10)
 * val openClosed = openClosedRangeOf(0, 10) // (0, 10]
 * val open = openOpenRangeOf(0, 10)         // (0, 10)
 *
 * // 하한 경계 포함 여부
 * 0 in closed     // true  — Closed 하한: 0 포함
 * 0 in closedOpen // true  — Closed 하한: 0 포함
 * 0 in openClosed // false — Open 하한: 0 미포함
 * 0 in open       // false — Open 하한: 0 미포함
 *
 * // 상한 경계 포함 여부
 * 10 in closed     // true  — Closed 상한: 10 포함
 * 10 in closedOpen // false — Open 상한: 10 미포함
 * 10 in openClosed // true  — Closed 상한: 10 포함
 * 10 in open       // false — Open 상한: 10 미포함
 * ```
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

    /**
     * 주어진 값이 이 범위에 포함되는지 확인합니다.
     * 경계 타입(Open/Closed)에 따라 포함 여부가 달라집니다.
     *
     * ```kotlin
     * val closed = closedClosedRangeOf(1, 5)  // [1, 5]
     * val open = openOpenRangeOf(1, 5)         // (1, 5)
     *
     * 1 in closed // true  — 하한 1이 Closed이므로 포함
     * 1 in open   // false — 하한 1이 Open이므로 미포함
     * 5 in closed // true  — 상한 5가 Closed이므로 포함
     * 5 in open   // false — 상한 5가 Open이므로 미포함
     * 3 in closed // true
     * 3 in open   // true
     * ```
     */
    operator fun contains(value: T): Boolean

    /**
     * 이 범위가 비어있는지 확인합니다.
     *
     * ```kotlin
     * closedClosedRangeOf(5, 3).isEmpty() // true  — 하한이 상한보다 크므로 빈 범위
     * closedClosedRangeOf(3, 5).isEmpty() // false
     * openOpenRangeOf(5, 5).isEmpty()     // true  — 양쪽 Open이면 동일 값도 빈 범위
     * ```
     */
    fun isEmpty(): Boolean
}

/**
 * 이 범위가 다른 범위 [other]를 완전히 포함하는지 확인합니다.
 * 경계 타입(Open/Closed)을 고려하여 정확하게 판단합니다.
 *
 * 예: `[0, 10].contains((2, 8))` → `true`, `(0, 10).contains([0, 10])` → `false`
 *
 * ```kotlin
 * val closed = closedClosedRangeOf(0, 10)  // [0, 10]
 * val open = openOpenRangeOf(0, 10)         // (0, 10)
 *
 * closed.contains(openOpenRangeOf(2, 8))    // true  — [0,10]은 (2,8)을 완전 포함
 * closed.contains(closedClosedRangeOf(0, 10)) // true  — 동일 Closed 범위 포함
 * open.contains(closedClosedRangeOf(0, 10)) // false — Open 범위는 경계값 0,10 미포함
 * open.contains(open)                       // true  — 동일 Open 범위 포함
 *
 * val closedOpen = closedOpenRangeOf(0, 10) // [0, 10)
 * closedOpen.contains(closed)               // false — 상한 10이 open이므로 [0,10] 미포함
 * closed.contains(closedOpen)               // true  — [0,10]은 [0,10)을 포함
 * ```
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
 *
 * ```kotlin
 * // Closed 범위끼리: 경계값에서 만나면 겹침
 * closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(5, 10)) // true  — 5에서 만남
 * closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(6, 10)) // false — 겹치지 않음
 * closedClosedRangeOf(0, 5).overlaps(closedClosedRangeOf(3, 8))  // true  — 3~5 구간 겹침
 *
 * // Open 경계에서는 경계값 자체를 공유해도 겹치지 않음
 * openOpenRangeOf(0, 5).overlaps(openOpenRangeOf(5, 10))         // false — 5가 양쪽에서 제외
 * openOpenRangeOf(0, 5).overlaps(openOpenRangeOf(3, 10))         // true  — 3~5 구간 겹침
 *
 * // 혼합 경계 타입
 * closedClosedRangeOf(0, 5).overlaps(openClosedRangeOf(5, 10))   // false — 5가 open side에서 제외
 * closedClosedRangeOf(0, 5).overlaps(closedOpenRangeOf(5, 10))   // true  — 5에서 만남(양쪽 inclusive)
 * closedOpenRangeOf(0, 5).overlaps(closedClosedRangeOf(5, 10))   // false — 5가 왼쪽에서 제외
 * ```
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
 *
 * ```kotlin
 * val ascending = listOf(
 *     openOpenRangeOf(0, 10),
 *     openOpenRangeOf(1, 5),
 *     openOpenRangeOf(12, 13),
 * )
 * ascending.isAscending()  // true — first 값이 0, 1, 12로 오름차순
 *
 * val notAscending = listOf(
 *     openOpenRangeOf(2, 10),
 *     openOpenRangeOf(1, 5),  // first=1이 이전 first=2보다 작으므로 정렬 아님
 *     openOpenRangeOf(5, 13),
 * )
 * notAscending.isAscending() // false
 *
 * emptyList<Range<Int>>().isAscending()            // true — 빈 컬렉션은 항상 오름차순
 * listOf(openOpenRangeOf(1, 5)).isAscending()      // true — 단일 원소는 항상 오름차순
 * ```
 */
fun <T: Comparable<T>> Iterable<Range<T>>.isAscending(): Boolean {
    val iterator = iterator()
    if (!iterator.hasNext()) return true

    var max = iterator.next().first
    while (iterator.hasNext()) {
        val range = iterator.next()
        if (max > range.first) {
            return false
        }
        if (range.first > max) {
            max = range.first
        }
    }
    return true
}
