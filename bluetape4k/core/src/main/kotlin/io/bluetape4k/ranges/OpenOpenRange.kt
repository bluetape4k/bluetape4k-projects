package io.bluetape4k.ranges


/**
 * 상, 하한 모두 포함되지 않는 범위를 표현합니다. (`startExclusive < x < endExclusive`)
 */
interface OpenOpenRange<T: Comparable<T>>: Range<T> {

    /**
     * 하한 (미포함)
     */
    val startExclusive: T

    /**
     * 상한 (미포함)
     */
    val endExclusive: T

    /**
     * 하한 (미포함)
     */
    override val first: T get() = startExclusive

    /**
     * 상한 (미포함)
     */
    override val last: T get() = endExclusive

    override val isStartInclusive: Boolean get() = false
    override val isEndInclusive: Boolean get() = false

    override fun contains(value: T): Boolean =
        value > startExclusive && value < endExclusive

    override fun isEmpty(): Boolean =
        startExclusive >= endExclusive
}

/**
 * 상, 하한 모두 포함되지 않는 범위를 표현합니다. (`startExclusive < x < endExclusive`)
 *
 * @param T
 * @property startExclusive 하한 (미포함)
 * @property endExclusive   상한 (미포함)
 */
data class DefaultOpenOpenRange<T: Comparable<T>>(
    override val startExclusive: T,
    override val endExclusive: T,
): OpenOpenRange<T> {
    override fun toString(): String = "($startExclusive..$endExclusive)"
}

/**
 * [OpenOpenRange]를 생성하는 팩토리 함수입니다.
 *
 * @param start 하한 (미포함)
 * @param end 상한 (미포함)
 */
fun <T: Comparable<T>> openOpenRangeOf(start: T, end: T): OpenOpenRange<T> =
    DefaultOpenOpenRange(start, end)

/**
 * [ClosedRange]를 [OpenOpenRange]로 변환합니다.
 *
 * **주의:** [ClosedRange]의 양쪽 경계가 모두 Exclusive로 변환되므로,
 * 원래 경계 값들은 범위에서 제외됩니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toOpenOpenRange(): OpenOpenRange<T> =
    DefaultOpenOpenRange(start, endInclusive)

/**
 * [Range]를 [OpenOpenRange]로 변환합니다.
 *
 * **주의:** 양쪽 경계가 모두 Exclusive로 변환되므로,
 * 원래 경계 값의 포함 여부가 변경될 수 있습니다.
 */
fun <T: Comparable<T>> Range<T>.toOpenOpenRange(): OpenOpenRange<T> =
    DefaultOpenOpenRange(first, last)