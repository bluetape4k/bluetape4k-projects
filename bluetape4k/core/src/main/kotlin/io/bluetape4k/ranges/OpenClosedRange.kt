package io.bluetape4k.ranges

/**
 * 하한은 포함 안되고, 상한은 포함되는 범위를 표현합니다. (`startExclusive < x <= endInclusive`)
 *
 * @property startExclusive 하한
 * @property endInclusive 상한
 */
interface OpenClosedRange<T: Comparable<T>>: Range<T> {

    /**
     * 하한 (미포함)
     */
    val startExclusive: T

    /**
     * 상한 (포함)
     */
    val endInclusive: T

    /**
     * 하한 (미포함)
     */
    override val first: T get() = startExclusive

    /**
     * 상한 (포함)
     */
    override val last: T get() = endInclusive

    override val isStartInclusive: Boolean get() = false
    override val isEndInclusive: Boolean get() = true

    override fun contains(value: T): Boolean =
        value > startExclusive && value <= endInclusive

    override fun isEmpty(): Boolean = endInclusive <= startExclusive
}

/**
 * 하한은 포함 안되고, 상한은 포함되는 범위를 표현합니다. (`startExclusive < x <= endInclusive`)
 *
 * @param T
 * @property startExclusive 하한 (미포함)
 * @property endInclusive   상한 (포함)
 */
data class DefaultOpenClosedRange<T: Comparable<T>>(
    override val startExclusive: T,
    override val endInclusive: T,
): OpenClosedRange<T> {

    override fun toString(): String = "($startExclusive..$endInclusive]"
}

/**
 * [OpenClosedRange]를 생성하는 팩토리 함수입니다.
 *
 * @param start 하한 (미포함)
 * @param end 상한 (포함)
 */
fun <T: Comparable<T>> openClosedRangeOf(start: T, end: T): OpenClosedRange<T> =
    DefaultOpenClosedRange(start, end)

/**
 * [ClosedRange]를 [OpenClosedRange]로 변환합니다.
 *
 * **주의:** [ClosedRange.start] 값이 그대로 `startExclusive`로 사용되므로,
 * 변환 후 원래 하한 값은 범위에서 제외됩니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toOpenClosedRange(): OpenClosedRange<T> =
    DefaultOpenClosedRange(start, endInclusive)

/**
 * [Range]를 [OpenClosedRange]로 변환합니다.
 *
 * **주의:** [Range.first] 값이 그대로 `startExclusive`로 사용되므로,
 * 원래 하한 값의 포함 여부가 변경될 수 있습니다.
 */
fun <T: Comparable<T>> Range<T>.toOpenClosedRange(): OpenClosedRange<T> =
    DefaultOpenClosedRange(first, last)