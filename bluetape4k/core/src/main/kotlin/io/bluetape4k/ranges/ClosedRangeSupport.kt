package io.bluetape4k.ranges

import java.math.BigDecimal
import java.math.BigInteger

/**
 * [ClosedRange]의 컬렉션이 오름차순으로 정렬되어 있는지 확인합니다.
 *
 * 예제:
 * ```kotlin
 * listOf(1..3, 5..7, 9..11).isAscending() // true  — 각 range의 start가 오름차순
 * listOf(1..3, 2..4, 5..7).isAscending()  // false — 2번째 start(2) < 1번째 start(1) 아님, 하지만 겹침
 * listOf(5..7, 1..3).isAscending()         // false — start가 역순
 * emptyList<ClosedRange<Int>>().isAscending() // true  — 빈 컬렉션은 오름차순으로 간주
 * ```
 *
 * @return 컬렉션이 비어있거나 각 원소의 start 값이 비내림차순이면 `true`
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
 *
 * 예제:
 * ```kotlin
 * val range = BigDecimal("1.5")..BigDecimal("3.5") // [1.5..3.5]
 * range.contains(BigDecimal("1.5")) // true  — 하한 포함
 * range.contains(BigDecimal("3.5")) // true  — 상한 포함
 * range.contains(BigDecimal("2.0")) // true
 * range.contains(BigDecimal("4.0")) // false
 * ```
 *
 * @return [this, endInclusive] 구간의 [ClosedClosedRange]
 */
operator fun BigDecimal.rangeTo(endInclusive: BigDecimal): ClosedClosedRange<BigDecimal> =
    DefaultClosedClosedRange(this, endInclusive)

/**
 * [BigInteger] 수형의 [ClosedClosedRange]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val range = BigInteger.ONE..BigInteger.TEN // [1..10]
 * range.contains(BigInteger.ONE)             // true  — 하한 포함
 * range.contains(BigInteger.TEN)             // true  — 상한 포함
 * range.contains(BigInteger.valueOf(5))      // true
 * range.contains(BigInteger.ZERO)            // false
 * ```
 *
 * @return [this, endInclusive] 구간의 [ClosedClosedRange]
 */
operator fun BigInteger.rangeTo(endInclusive: BigInteger): ClosedClosedRange<BigInteger> =
    DefaultClosedClosedRange(this, endInclusive)
