package io.bluetape4k.math.special

import io.bluetape4k.math.MathConsts.EPSILON
import io.bluetape4k.math.commons.abs
import io.bluetape4k.math.commons.positiveEpsilon
import io.bluetape4k.math.commons.square
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Numerically stable exponential minus one, i.e. `x -> exp(x)-1``
 *
 * @param power
 * @return
 */
fun exponentialMinusOne(power: Double): Double {
    val x = power.absoluteValue
    if (x > 0.1) {
        return exp(power) - 1.0
    }

    if (x < x.positiveEpsilon()) {
        return x
    }

    // Series expansion to x^k / k!
    var k = 0
    var term = 1.0

    return series {
        term *= power / ++k
        term
    }
}

/**
 * Numerically stable hypotenuse of a right angle triangle,
 * i.e.
 * ```kotlin
 *   (a,b) -> sqrt(a^2 + b^2)
 * ```
 *
 * @param a
 * @param b
 * @return
 */
fun hypotenuse(a: Double, b: Double): Double {
    if (a.abs() > b.abs()) {
        val r = b / a
        return a.abs() * sqrt(1 + r.square())
    }

    if ((b - 0.0).abs() > EPSILON) {
        val r = a / b
        return b.abs() * sqrt(1.0 + r.square())
    }
    return 0.0
}

/**
 * Numerical stable series summation
 *
 * @param nextSummand provides the summands sequentially
 * @return summation
 */
inline fun series(nextSummand: () -> Double): Double {
    val factor = 2.0.pow(16)
    var compensation = 0.0

    var sum = nextSummand()
    var current: Double

    do {
        current = nextSummand()
        val y = current - compensation
        val t = sum + y
        compensation = t - sum - y
        sum = t
    } while (sum.abs() < (factor * current).abs())

    return sum
}
