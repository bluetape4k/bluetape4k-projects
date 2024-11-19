package io.bluetape4k.math.commons

import io.bluetape4k.math.MathConsts.EPSILON
import io.bluetape4k.math.MathConsts.FLOAT_EPSILON
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.absoluteValue

fun Double.approximateEqual(that: Double, epsilon: Double = EPSILON): Boolean =
    abs(this - that) < epsilon.absoluteValue

fun Float.approximateEqual(that: Float, epsilon: Float = FLOAT_EPSILON): Boolean =
    abs(this - that) < abs(epsilon)

fun BigDecimal.approximateEqual(that: BigDecimal, epsilon: BigDecimal = EPSILON.toBigDecimal()): Boolean =
    (this - that).abs() < epsilon.abs()

fun Iterable<Double>.filterApproximate(
    that: Double,
    epsilon: Double = EPSILON,
): List<Double> {
    return filter { it.approximateEqual(that, epsilon) }
}

fun Iterable<Float>.filterApproximate(
    that: Float,
    epsilon: Float = FLOAT_EPSILON,
): List<Float> {
    return filter { it.approximateEqual(that, epsilon) }
}

fun Iterable<BigDecimal>.filterApproximate(
    that: BigDecimal,
    epsilon: BigDecimal = EPSILON.toBigDecimal(),
): List<BigDecimal> {
    return filter { it.approximateEqual(that, epsilon) }
}

fun Sequence<Double>.filterApproximate(
    that: Double,
    epsilon: Double = EPSILON,
): Sequence<Double> {
    return filter { it.approximateEqual(that, epsilon) }
}

fun Sequence<Float>.filterApproximate(
    that: Float,
    epsilon: Float = FLOAT_EPSILON,
): Sequence<Float> {
    return filter { it.approximateEqual(that, epsilon) }
}

fun Sequence<BigDecimal>.filterApproximate(
    that: BigDecimal,
    epsilon: BigDecimal = EPSILON.toBigDecimal(),
): Sequence<BigDecimal> {
    return filter { it.approximateEqual(that, epsilon) }
}
