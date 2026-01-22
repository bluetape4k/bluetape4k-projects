package io.bluetape4k.math

inline fun <T: Any> Sequence<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    rangeStart: Double? = null,
): BinModel<List<T>, Double> =
    asIterable().binByDouble(binSize, valueMapper, rangeStart)

inline fun <T: Any> Iterable<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    rangeStart: Double? = null,
): BinModel<List<T>, Double> =
    binByDouble(binSize, valueMapper, { it }, rangeStart)

inline fun <T: Any, G: Any> Iterable<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    crossinline groupOp: (List<T>) -> G,
    rangeStart: Double? = null,
): BinModel<G, Double> =
    binByComparable({ it + binSize }, valueMapper, groupOp, rangeStart)
