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
 * [ClosedRange]를 [OpenOpenRange]로 변환합니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toOpenOpenRange(): OpenOpenRange<T> =
    DefaultOpenOpenRange(start, endInclusive)

/**
 * [Range]를 [OpenOpenRange]로 변환합니다.
 */
fun <T: Comparable<T>> Range<T>.toOpenOpenRange(): OpenOpenRange<T> =
    DefaultOpenOpenRange(first, last)
