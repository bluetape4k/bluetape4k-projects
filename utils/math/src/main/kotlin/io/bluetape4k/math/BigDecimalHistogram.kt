package io.bluetape4k.math

import java.math.BigDecimal

/**
 * BigDecimal 값을 기준으로 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Price(val value: BigDecimal)
 * val prices = sequenceOf(Price(BigDecimal("1.5")), Price(BigDecimal("2.5")), Price(BigDecimal("3.5")))
 * val bins = prices.binByBigDecimal(BigDecimal("1.0")) { it.value }
 * // BigDecimal 범위로 구분된 BinModel
 * ```
 */
inline fun <T: Any> Sequence<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    rangeStart: BigDecimal? = null,
): BinModel<List<T>, BigDecimal> =
    asIterable().binByBigDecimal(binSize, valueMapper, { it }, rangeStart)

/**
 * BigDecimal 값을 기준으로 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Price(val value: BigDecimal)
 * val prices = listOf(Price(BigDecimal("1.5")), Price(BigDecimal("2.5")), Price(BigDecimal("3.5")))
 * val bins = prices.binByBigDecimal(BigDecimal("1.0")) { it.value }
 * // BigDecimal 범위로 구분된 BinModel
 * ```
 */
inline fun <T: Any> Iterable<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    rangeStart: BigDecimal? = null,
): BinModel<List<T>, BigDecimal> =
    binByBigDecimal(binSize, valueMapper, { it }, rangeStart)

/**
 * BigDecimal 값을 기준으로 그룹화된 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Price(val value: BigDecimal)
 * val prices = listOf(Price(BigDecimal("1.5")), Price(BigDecimal("2.5")), Price(BigDecimal("3.5")))
 * val bins = prices.binByBigDecimal(BigDecimal("1.0"), { it.value }, { it.size })
 * // 각 bin에 포함된 요소 수를 집계한 BinModel
 * ```
 */
inline fun <T: Any, G: Any> Iterable<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    crossinline groupOp: (List<T>) -> G,
    rangeStart: BigDecimal? = null,
): BinModel<G, BigDecimal> {
    assert(count() > 0) { "Collection must not be empty." }
    return binByComparable({ it + binSize }, valueMapper, groupOp, rangeStart)
}
