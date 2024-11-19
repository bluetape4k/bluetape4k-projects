package io.bluetape4k.math.fraction

import io.bluetape4k.support.toBigInt
import org.apache.commons.math3.fraction.BigFraction

operator fun BigFraction.plus(scalar: Number): BigFraction = this.add(scalar.toLong())
operator fun BigFraction.plus(that: BigFraction): BigFraction = this.add(that)

operator fun BigFraction.minus(scalar: Number): BigFraction = this.subtract(scalar.toLong())
operator fun BigFraction.minus(that: BigFraction): BigFraction = this.subtract(that)

operator fun BigFraction.times(scalar: Number): BigFraction = this.multiply(scalar.toLong())
operator fun BigFraction.times(that: BigFraction): BigFraction = this.multiply(that)

operator fun BigFraction.div(scalar: Number): BigFraction = this.divide(scalar.toLong())
operator fun BigFraction.div(that: BigFraction): BigFraction = this.divide(that)


fun <N: Number> bigFractionOf(numerator: N): BigFraction =
    BigFraction(numerator.toBigInt())

fun <N: Number> bigFractionOf(numerator: N, denominator: N): BigFraction =
    BigFraction(numerator.toBigInt(), denominator.toBigInt())

fun bigFractionOf(value: Double, epsilon: Double = 0.0, maxIterations: Int = 100): BigFraction =
    BigFraction(value, epsilon, maxIterations)

fun <N: Number> reducedBigFractionOf(numerator: N, denominator: N): BigFraction =
    BigFraction.getReducedFraction(numerator.toInt(), denominator.toInt())
