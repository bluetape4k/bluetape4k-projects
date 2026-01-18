package io.bluetape4k.ranges

/**
 * 하한, 상한 모두 포함하는 Range ( `startInclusive <= x <= endInclusive` )
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

    override fun contains(value: T): Boolean =
        value in startInclusive..endInclusive

    override fun isEmpty(): Boolean =
        startInclusive > endInclusive
}

/**
 * 기본 [ClosedClosedRange] 구현체 (`start <= x <= end`)
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

    override fun isEmpty(): Boolean = startInclusive >= endInclusive
    override fun toString(): String = "[$startInclusive..$endInclusive]"
}

/**
 * [Range]를 [ClosedClosedRange]로 변환합니다.
 */
fun <T: Comparable<T>> Range<T>.toClosedClosedRange(): ClosedClosedRange<T> =
    DefaultClosedClosedRange(first, last)

/**
 * [ClosedRange]를 [ClosedClosedRange]로 변환합니다.
 */
fun <T: Comparable<T>> ClosedRange<T>.toClosedClosedRange(): ClosedClosedRange<T> =
    DefaultClosedClosedRange(start, endInclusive)
