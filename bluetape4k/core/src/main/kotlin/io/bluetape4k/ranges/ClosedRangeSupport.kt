package io.bluetape4k.ranges

import java.math.BigDecimal
import java.math.BigInteger

/**
 * [ClosedRange]의 컬렉션이 오름차순으로 정렬되어 있는지 확인합니다.
 */
fun <T: Comparable<T>> Iterable<ClosedRange<T>>.isAscending(): Boolean {
    val first = firstOrNull() ?: return true

    var max = first.start
    return drop(1).fold(true) { isAscending, elem ->
        val newAscending = isAscending && (max <= elem.start)
        max = maxOf(max, elem.start)
        newAscending
    }
}

/**
 * [BigDecimal] 수형의 [ClosedClosedRange]를 생성합니다.
 */
@SinceKotlin("1.1")
operator fun BigDecimal.rangeTo(endInclusive: BigDecimal): ClosedClosedRange<BigDecimal> =
    DefaultClosedClosedRange(this, endInclusive)

/**
 * [BigInteger] 수형의 [ClosedClosedRange]를 생성합니다.
 */
@SinceKotlin("1.1")
operator fun BigInteger.rangeTo(endInclusive: BigInteger): ClosedClosedRange<BigInteger> =
    DefaultClosedClosedRange(this, endInclusive)
