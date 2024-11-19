package io.bluetape4k.math.linear

import org.apache.commons.math3.linear.AnyMatrix
import org.apache.commons.math3.linear.RealMatrix

operator fun RealMatrix.plus(rm: RealMatrix): RealMatrix = add(rm)
operator fun <N: Number> RealMatrix.plus(scalar: N): RealMatrix = scalarAdd(scalar.toDouble())

operator fun RealMatrix.minus(rm: RealMatrix): RealMatrix = subtract(rm)
operator fun <N: Number> RealMatrix.minus(scalar: N): RealMatrix = scalarAdd(-scalar.toDouble())

operator fun RealMatrix.times(rm: RealMatrix): RealMatrix = multiply(rm)
operator fun <N: Number> RealMatrix.times(scalar: N): AnyMatrix = scalarMultiply(scalar.toDouble())

operator fun RealMatrix.div(rm: RealMatrix): RealMatrix = multiply(rm.inverse())
operator fun <N: Number> RealMatrix.div(scalar: N): AnyMatrix = scalarMultiply(1.0 / scalar.toDouble())
