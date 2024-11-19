package io.bluetape4k.math

import java.math.BigDecimal

inline fun <T: Any> Sequence<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    rangeStart: BigDecimal? = null,
): BinModel<List<T>, BigDecimal> =
    asIterable().binByBigDecimal(binSize, valueMapper, { it }, rangeStart)

inline fun <T: Any> Iterable<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    rangeStart: BigDecimal? = null,
): BinModel<List<T>, BigDecimal> =
    binByBigDecimal(binSize, valueMapper, { it }, rangeStart)


inline fun <T: Any, G: Any> Iterable<T>.binByBigDecimal(
    binSize: BigDecimal,
    valueMapper: (T) -> BigDecimal,
    crossinline groupOp: (List<T>) -> G,
    rangeStart: BigDecimal? = null,
): BinModel<G, BigDecimal> {
    assert(count() > 0) { "Collection must not be empty." }

    return binByComparable({ it + binSize }, valueMapper, groupOp, rangeStart)
}
