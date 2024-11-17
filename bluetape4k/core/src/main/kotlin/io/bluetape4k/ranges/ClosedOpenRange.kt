package io.bluetape4k.ranges

/**
 * 시작값은 포함, 끝 값은 미포함하는 범위를 나타냅니다. (`startInclusive <= x < endExclusive`)
 *
 * @param T
 * @constructor Create empty Closed open range
 */
interface ClosedOpenRange<T: Comparable<T>>: Range<T> {

    /**
     * 하한 (포함)
     */
    val startInclusive: T

    /**
     * 상한 (미포함)
     */
    val endExclusive: T

    /**
     * 하한 (포함)
     */
    override val first: T get() = startInclusive

    /**
     * 상한 (미포함)
     */
    override val last: T get() = endExclusive

    override fun contains(value: T): Boolean =
        value >= startInclusive && value < endExclusive

    override fun isEmpty(): Boolean = startInclusive >= endExclusive
}

/**
 * [ClosedOpenRange]의 기본 구현체 (`start <= x < end`)
 *
 * @param T
 * @property startInclusive 하한 (포함)
 * @property endExclusive   상한 (미포함)
 * @constructor Create empty Default closed open range
 */
data class DefaultClosedOpenRange<T: Comparable<T>>(
    override val startInclusive: T,
    override val endExclusive: T,
): ClosedOpenRange<T> {

    override fun toString(): String = "[$startInclusive..$endExclusive)"
}

infix fun <T: Comparable<T>> T.until(endExclusive: T): ClosedOpenRange<T> =
    DefaultClosedOpenRange(this, endExclusive)

/**
 * [ClosedRange]를 [ClosedOpenRange]로 변환합니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toClosedOpenRange(): ClosedOpenRange<T> =
    DefaultClosedOpenRange(start, endInclusive)

/**
 * [Range]를 [ClosedOpenRange]로 변환합니다.
 */
fun <T: Comparable<T>> Range<T>.toClosedOpenRange(): ClosedOpenRange<T> =
    DefaultClosedOpenRange(first, last)

data class ClosedOpenDoubleRange(
    override val startInclusive: Double,
    override val endExclusive: Double,
): ClosedOpenRange<Double>

/**
 * [Double] 범위를 [ClosedOpenDoubleRange]로 변환합니다.
 */
infix fun Double.closedOpenRange(endExclusive: Double): ClosedOpenDoubleRange =
    ClosedOpenDoubleRange(this, endExclusive)
