package io.bluetape4k.math.linear

import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector

operator fun RealVector.get(index: Int): Double = getEntry(index)
operator fun RealVector.set(index: Int, value: Double) = setEntry(index, value)

operator fun RealVector.plus(v: RealVector): RealVector = add(v)
operator fun <N: Number> RealVector.plus(scalar: Number): RealVector = mapAdd(scalar.toDouble())

operator fun RealVector.minus(v: RealVector): RealVector = subtract(v)
operator fun <N: Number> RealVector.minus(scalar: Number): RealVector = mapSubtract(scalar.toDouble())

operator fun RealVector.times(v: RealVector): RealVector = ebeMultiply(v)
operator fun <N: Number> RealVector.times(scalar: Number): RealVector = mapMultiply(scalar.toDouble())

operator fun RealVector.div(v: RealVector): RealVector = ebeDivide(v)
operator fun <N: Number> RealVector.div(scalar: Number): RealVector = mapDivide(scalar.toDouble())

fun RealVector.toArrayRealVector(): ArrayRealVector = ArrayRealVector(this)
