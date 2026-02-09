package io.bluetape4k.ranges

/**
 * 시작값은 포함, 끝 값은 미포함하는 범위를 나타냅니다. (`startInclusive <= x < endExclusive`)
 *
 * @param T
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

    override val isStartInclusive: Boolean get() = true
    override val isEndInclusive: Boolean get() = false

    override fun contains(value: T): Boolean =
        value in startInclusive..<endExclusive

    override fun isEmpty(): Boolean = startInclusive >= endExclusive
}

/**
 * [ClosedOpenRange]의 기본 구현체 (`start <= x < end`)
 *
 * @param T
 * @property startInclusive 하한 (포함)
 * @property endExclusive   상한 (미포함)
 */
data class DefaultClosedOpenRange<T: Comparable<T>>(
    override val startInclusive: T,
    override val endExclusive: T,
): ClosedOpenRange<T> {

    override fun toString(): String = "[$startInclusive..$endExclusive)"
}

/**
 * [ClosedOpenRange]를 생성하는 팩토리 함수입니다.
 *
 * @param start 하한 (포함)
 * @param end 상한 (미포함)
 */
fun <T: Comparable<T>> closedOpenRangeOf(start: T, end: T): ClosedOpenRange<T> =
    DefaultClosedOpenRange(start, end)

infix fun <T: Comparable<T>> T.until(endExclusive: T): ClosedOpenRange<T> =
    DefaultClosedOpenRange(this, endExclusive)

/**
 * [ClosedRange]를 [ClosedOpenRange]로 변환합니다.
 *
 * **주의:** [ClosedRange]의 `endInclusive` 값이 그대로 `endExclusive`로 사용되므로,
 * 변환 후 원래 상한 값은 범위에서 제외됩니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toClosedOpenRange(): ClosedOpenRange<T> =
    DefaultClosedOpenRange(start, endInclusive)

/**
 * [Range]를 [ClosedOpenRange]로 변환합니다.
 *
 * **주의:** [Range.last] 값이 그대로 `endExclusive`로 사용되므로,
 * 원래 상한 값의 포함 여부가 변경될 수 있습니다.
 */
fun <T: Comparable<T>> Range<T>.toClosedOpenRange(): ClosedOpenRange<T> =
    DefaultClosedOpenRange(first, last)
