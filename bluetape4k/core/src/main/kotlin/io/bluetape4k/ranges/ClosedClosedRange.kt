package io.bluetape4k.ranges

/**
 * 하한, 상한 모두 포함하는 Range ( `startInclusive <= x <= endInclusive` )
 *
 * 예제:
 * ```kotlin
 * val range = closedClosedRangeOf(1, 5) // [1..5]
 * range.contains(1) // true  — 하한 포함
 * range.contains(5) // true  — 상한 포함
 * range.contains(3) // true  — 내부 값 포함
 * range.contains(0) // false — 하한 미만
 * range.contains(6) // false — 상한 초과
 * ```
 *
 * @param T 요소의 수형
 */
interface ClosedClosedRange<T: Comparable<T>>: Range<T>, ClosedRange<T> {

    /**
     * 시작 값 (포함)
     */
    val startInclusive: T

    /**
     * 끝 값 (포함)
     */
    override val endInclusive: T

    override val first: T get() = startInclusive
    override val last: T get() = endInclusive

    override val isStartInclusive: Boolean get() = true
    override val isEndInclusive: Boolean get() = true

    override fun contains(value: T): Boolean =
        value in startInclusive..endInclusive

    override fun isEmpty(): Boolean =
        startInclusive > endInclusive
}

/**
 * 기본 [ClosedClosedRange] 구현체 (`start <= x <= end`)
 *
 * 예제:
 * ```kotlin
 * val range = DefaultClosedClosedRange(1, 5)
 * range.toString()    // "[1..5]"
 * range.isEmpty()     // false
 * range.contains(1)   // true  — 하한 포함
 * range.contains(5)   // true  — 상한 포함
 * DefaultClosedClosedRange(5, 1).isEmpty() // true  — start > end
 * ```
 *
 * @param T
 * @property startInclusive 하한 (포함)
 * @property endInclusive 상한 (포함)
 */
data class DefaultClosedClosedRange<T: Comparable<T>>(
    override val startInclusive: T,
    override val endInclusive: T,
): ClosedClosedRange<T>, ClosedRange<T> by startInclusive..endInclusive {

    override fun contains(value: T): Boolean =
        value in startInclusive..endInclusive

    override fun isEmpty(): Boolean = startInclusive > endInclusive
    override fun toString(): String = "[$startInclusive..$endInclusive]"
}

/**
 * [ClosedClosedRange]를 생성하는 팩토리 함수입니다.
 *
 * 예제:
 * ```kotlin
 * val range = closedClosedRangeOf(1, 5) // [1..5]
 * range.contains(1) // true
 * range.contains(5) // true
 * ```
 *
 * @param start 하한 (포함)
 * @param end 상한 (포함)
 * @return [start, end] 범위를 나타내는 [ClosedClosedRange]
 */
fun <T: Comparable<T>> closedClosedRangeOf(start: T, end: T): ClosedClosedRange<T> =
    DefaultClosedClosedRange(start, end)

/**
 * [Range]를 [ClosedClosedRange]로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val range = openClosedRangeOf(1, 5).toClosedClosedRange() // [1..5] — first/last 값 그대로 사용
 * range.isStartInclusive // true
 * range.isEndInclusive   // true
 * ```
 *
 * @return [first, last] 구간의 [ClosedClosedRange]
 */
fun <T: Comparable<T>> Range<T>.toClosedClosedRange(): ClosedClosedRange<T> =
    DefaultClosedClosedRange(first, last)

/**
 * [ClosedRange]를 [ClosedClosedRange]로 변환합니다.
 *
 * 예제:
 * ```kotlin
 * val range = (1..5).toClosedClosedRange() // [1..5]
 * range.isStartInclusive // true
 * range.isEndInclusive   // true
 * range.contains(1)      // true
 * range.contains(5)      // true
 * ```
 *
 * @return [start, endInclusive] 구간의 [ClosedClosedRange]
 */
fun <T: Comparable<T>> ClosedRange<T>.toClosedClosedRange(): ClosedClosedRange<T> =
    DefaultClosedClosedRange(start, endInclusive)