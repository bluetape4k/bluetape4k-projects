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
 * [ClosedRange]를 [OpenClosedRange]로 변환합니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toOpenClosedRange(): OpenClosedRange<T> =
    DefaultOpenClosedRange(start, endInclusive)

/**
 * [Range]를 [OpenClosedRange]로 변환합니다.
 */
fun <T: Comparable<T>> Range<T>.toOpenClosedRange(): OpenClosedRange<T> =
    DefaultOpenClosedRange(first, last)
