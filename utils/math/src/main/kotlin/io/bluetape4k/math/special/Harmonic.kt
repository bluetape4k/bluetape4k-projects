package io.bluetape4k.math.special

import io.bluetape4k.math.MathConsts.EULER_MASCHERONI
import io.bluetape4k.math.MathConsts.Pi
import io.bluetape4k.math.commons.approximateEqual
import io.bluetape4k.math.commons.isSpecialCase
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.tan

/**
 * Computes the `x`-th Harmonic number.
 *
 * @param x  The Harmonic number which needs to be computed.
 * @return   The t'th Harmonic number.
 */
fun harmonic(x: Int): Double = EULER_MASCHERONI + diGamma(x + 1.0)

/**
 * Compute the generalized harmonic number of order n of m. (1 + 1/2^m + 1/3^m + ... + 1/n^m)
 *
 * @param n Order parameter
 * @param m Power parameter
 * @return General Harmonic number
 */
fun generalHarmonic(n: Int, m: Double): Double {
    var sum = 0.0
    repeat(n) {
        sum += (it + 1).toDouble().pow(-m)
    }
    return sum
}

/**
 * Computes the Digamma function which is mathematically defined as the derivative of the logarithm of the gamma function.
 * This implementation is based on
 *     Jose Bernardo
 *     Algorithm AS 103:
 *     Psi ( Digamma ) Function,
 *     Applied Statistics,
 *     Volume 25, Number 3, 1976, pages 315-317.
 * Using the modifications as in Tom Minka's lightspeed toolbox.
 *
 * @param x The argument of the digamma function.
 * @return The value of the DiGamma function at `x`
 */
fun diGamma(x: Double): Double {

    val C = 12.0
    val D1 = -0.57721566490153286
    val D2 = 1.6449340668482264365
    val S = 1e-6
    val S3 = 1.0 / 12.0
    val S4 = 1.0 / 120.0
    val S5 = 1.0 / 252.0
    val S6 = 1.0 / 240.0
    val S7 = 1.0 / 132.0

    if (x.isInfinite() || x.isNaN()) {
        return Double.NaN
    }

    // Handle special cases.
    if (x <= 0 && floor(x).approximateEqual(x)) {
        return Double.NEGATIVE_INFINITY
    }

    // Use inversion formula for negative numbers.
    if (x < 0.0) {
        return diGamma(1.0 - x) + Pi / tan(-Pi * x)
    }

    if (x <= S) {
        return D1 - (1.0 / x) + (D2 * x)
    }

    var x1 = x
    var result = 0.0
    while (x < C) {
        result -= 1.0 / x1
        x1++
    }

    if (x1 >= C) {
        var r = 1.0 / x1
        result += ln(x) - (0.5 * r)
        r *= r

        result -= r * (S3 - (r * (S4 - (r * (S5 - (r * (S6 - (r * S7))))))))
    }
    return result
}

/**
 * Computes the inverse Digamma function: this is the inverse of the logarithm of the gamma function.
 * This function will only return solutions that are positive.
 *
 * This implementation is based on the bisection method.
 *
 * @param p  The argument of the inverse digamma function.
 * @return   The positive solution to the inverse DiGamma function at `p`
 */
fun diGammaInv(p: Double): Double {
    if (p.isSpecialCase()) {
        return exp(p)
    }

    var x = exp(p)
    var d = 1.0
    do {
        x += d * sign(p - diGamma(x))
        d /= 2.0
    } while (d > 1e-15)

    return x
}

/**
 * logit 함수를 계산합니다.
 * see: http://en.wikipedia.org/wiki/Logit
 *
 * @param p  The parameter for which to compute the logit function. This number should be between 0 and 1.
 * @return
 */
fun logit(p: Double): Double {
    require(p in 0.0..1.0) { "p[$p] 값은 [0,1] 사이어야 합니다." }
    return ln(p / (1.0 - p))
}

/**
 * Computes the logistic function.
 * see: http://en.wikipedia.org/wiki/Logistic
 *
 * @param p
 * @return
 */
fun logistic(p: Double): Double {
    return 1.0 / (exp(-p) + 1.0)
}
