package io.bluetape4k.math.special

import io.bluetape4k.support.assertPositiveNumber
import org.apache.commons.math3.special.Gamma.logGamma
import kotlin.math.exp

fun betaLn(x: Double, y: Double): Double {
    x.assertPositiveNumber("x")
    y.assertPositiveNumber("y")

    return logGamma(x) + logGamma(y) - logGamma(x + y)
}

fun betaLn(xs: Sequence<Double>, ys: Sequence<Double>): Sequence<Double> =
    xs.zip(ys).map { (x, y) -> betaLn(x, y) }

fun betaLn(xs: Iterable<Double>, ys: Iterable<Double>): DoubleArray {
    val results = mutableListOf<Double>()
    val xe = xs.iterator()
    val ye = ys.iterator()
    while (xe.hasNext() && ye.hasNext()) {
        results.add(betaLn(xe.next(), ye.next()))
    }
    return results.toDoubleArray()
}


fun betaLn(xs: DoubleArray, ys: DoubleArray): DoubleArray {
    val minSize = minOf(xs.size, ys.size)
    return DoubleArray(minSize) {
        betaLn(xs[it], ys[it])
    }
}

fun betaLn(xs: List<Double>, ys: List<Double>): List<Double> {
    val minSize = minOf(xs.size, ys.size)
    return List(minSize) {
        betaLn(xs[it], ys[it])
    }
}

fun beta(x: Double, y: Double): Double = exp(betaLn(x, y))

fun beta(xs: Sequence<Double>, ys: Sequence<Double>): Sequence<Double> {
    return betaLn(xs, ys).map { exp(it) }
}

fun beta(xs: Iterable<Double>, ys: Iterable<Double>): DoubleArray {
    return betaLn(xs, ys).map { exp(it) }.toDoubleArray()
}

fun beta(xs: DoubleArray, ys: DoubleArray): DoubleArray {
    val minSize = minOf(xs.size, ys.size)
    return DoubleArray(minSize) {
        beta(xs[it], ys[it])
    }
}

fun beta(xs: List<Double>, ys: List<Double>): List<Double> {
    val minSize = minOf(xs.size, ys.size)
    return List(minSize) { beta(xs[it], ys[it]) }
}
