package io.bluetape4k.math.special

import org.apache.commons.math3.special.BesselJ
import org.apache.commons.math3.special.Beta
import org.apache.commons.math3.special.Erf
import org.apache.commons.math3.special.Gamma

private const val DEFAULT_EPSILON: Double = 1.0e-14

/**
 * Returns the first Bessel function, \(J_{order}(x)\).
 *
 * @param order Order of the Bessel function
 * @return Value of Bessel function of the first kind.
 */
fun Double.besselj(order: Double) = BesselJ.value(order, this)

fun Double.logBeta(q: Double) = Beta.logBeta(this, q)

fun regularizedBeta(
    x: Double,
    a: Double,
    b: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double =
    Beta.regularizedBeta(x, a, b, epsilon, maxIterations)

fun Double.erf(): Double = Erf.erf(this)
fun Double.erf(x2: Double): Double = Erf.erf(this, x2)

fun Double.erfInv(): Double = Erf.erfInv(this)
fun Double.erfc() = Erf.erfc(this)
fun Double.erfcInv() = Erf.erfcInv(this)

fun Double.gamma() = Gamma.gamma(this)
fun Double.digamma() = Gamma.digamma(this)
fun Double.trigamma() = Gamma.trigamma(this)
fun Double.logGamma() = Gamma.logGamma(this)
fun Double.logGamma1p() = Gamma.logGamma1p(this)

fun regularizedGammaP(
    a: Double,
    x: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double {
    return Gamma.regularizedGammaP(a, x, epsilon, maxIterations)
}

fun regularizedGammaQ(
    a: Double,
    x: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double {
    return Gamma.regularizedGammaQ(a, x, epsilon, maxIterations)
}
