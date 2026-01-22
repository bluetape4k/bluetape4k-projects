package io.bluetape4k.math.fraction

import org.apache.commons.math3.fraction.Fraction

operator fun Fraction.plus(scalar: Number): Fraction = this.add(scalar.toInt())
operator fun Fraction.plus(that: Fraction): Fraction = this.add(that)

operator fun Fraction.minus(scalar: Number): Fraction = this.subtract(scalar.toInt())
operator fun Fraction.minus(that: Fraction): Fraction = this.subtract(that)

operator fun Fraction.times(scalar: Number): Fraction = this.multiply(scalar.toInt())
operator fun Fraction.times(that: Fraction): Fraction = this.multiply(that)

operator fun Fraction.div(scalar: Number): Fraction = this.divide(scalar.toInt())
operator fun Fraction.div(that: Fraction): Fraction = this.divide(that)


fun <N: Number> fractionOf(numerator: N): Fraction =
    Fraction(numerator.toInt())

fun <N: Number> fractionOf(numerator: N, denominator: N): Fraction =
    Fraction(numerator.toInt(), denominator.toInt())

fun fractionOf(value: Double, epsilon: Double = 0.0, maxIterations: Int = 100): Fraction =
    Fraction(value, epsilon, maxIterations)

fun <N: Number> reducedFractionOf(numerator: N, denominator: N): Fraction =
    Fraction.getReducedFraction(numerator.toInt(), denominator.toInt())
