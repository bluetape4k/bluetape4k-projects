package io.bluetape4k.math.special

import io.bluetape4k.math.commons.approximateEqual
import io.bluetape4k.support.assertZeroOrPositiveNumber
import org.apache.commons.math3.special.Gamma.gamma
import org.apache.commons.math3.special.Gamma.logGamma
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

private const val GammaN: Int = 10
private const val GammaR: Double = 10.900511

private val GammaDK = doubleArrayOf(
    2.48574089138753565546e-5,
    1.05142378581721974210,
    -3.45687097222016235469,
    4.51227709466894823700,
    -2.98285225323576655721,
    1.05639711577126713077,
    -1.95428773191645869583e-1,
    1.70970543404441224307e-2,
    -5.71926117404305781283e-4,
    4.63399473359905636708e-6,
    -2.71994908488607703910e-9
)

/**
 * Returns the upper incomplete regularized gamma function
 *
 * @param a The argument for the gamma function.
 * @param x The lower integral limit.
 * @return The upper incomplete regularized gamma function.
 */
fun gammaUpperRegularized(a: Double, x: Double): Double {
    val igammaepsilon = 0.000000000000001
    val igammabignumber = 4503599627370496.0
    val igammabignumberinv = 2.22044604925031308085 * 0.0000000000000001
    var ans: Double
    var ax: Double
    var c: Double
    var yc: Double
    var r: Double
    var t: Double
    var y: Double
    var z: Double
    var pk: Double
    var pkm1: Double
    var pkm2: Double
    var qk: Double
    var qkm1: Double
    var qkm2: Double

    if (x <= 0 || a <= 0) {
        return 1.0
    }

    if (x < 1 || x < a) {
        return 1 - gammaLowerRegularized(a, x)
    }

    ax = a * ln(x) - x - logGamma(a)
    if (ax < -709.78271289338399) {
        return 0.0
    }

    ax = exp(ax)
    y = 1 - a
    z = x + y + 1
    c = 0.0
    pkm2 = 1.0
    qkm2 = x
    pkm1 = x + 1
    qkm1 = z * x
    ans = pkm1 / qkm1
    do {
        c += 1
        y += 1
        z += 2
        yc = y * c
        pk = pkm1 * z - pkm2 * yc
        qk = qkm1 * z - qkm2 * yc

        if (!qk.approximateEqual(0.0)) {
            r = pk / qk
            t = Math.abs((ans - r) / r)
            ans = r
        } else {
            t = 1.0
        }

        pkm2 = pkm1
        pkm1 = pk
        qkm2 = qkm1
        qkm1 = qk

        if (abs(pk) > igammabignumber) {
            pkm2 *= igammabignumberinv
            pkm1 *= igammabignumberinv
            qkm2 *= igammabignumberinv
            qkm1 *= igammabignumberinv
        }
    } while (t > igammaepsilon)

    return ans * ax
}

/**
 * Returns the upper incomplete regularized gamma function
 * `P(a,x) = 1/Gamma(a) * int(exp(-t)t^(a-1),t=0..x) for real a > 0, x > 0.`
 *
 * @param a The argument for the gamma function.
 * @param x The lower integral limit.
 * @return The upper incomplete gamma function.
 */
fun gammaUpperIncomplete(a: Double, x: Double): Double {
    return gammaUpperRegularized(a, x) * gamma(a)
}

/**
 * Returns the lower incomplete regularized gamma function
 * `P(a,x) = 1/Gamma(a) * int(exp(-t)t^(a-1),t=0..x) for real a > 0, x > 0.`
 *
 * @param a The argument for the gamma function.
 * @param x The upper integral limit.
 * @return The lower incomplete gamma function.
 */
fun gammaLowerIncomplete(a: Double, x: Double): Double {
    return gammaLowerRegularized(a, x) * gamma(a)
}

private const val Epsilon = 0.000000000000001
private const val BigNumber = 4503599627370496.0
private const val BigNumberInverse = 2.22044604925031308085e-16


/**
 * Returns the lower incomplete gamma function
 *
 * `gamma(a,x) = int(exp(-t)t^(a-1),t=0..x) for real a > 0, x > 0.`
 *
 * @param a The argument for the gamma function.
 * @param x The upper integral limit.
 * @return The lower incomplete gamma function.
 */
fun gammaLowerRegularized(a: Double, x: Double): Double {
    a.assertZeroOrPositiveNumber("a")
    x.assertZeroOrPositiveNumber("x")

    if (a.approximateEqual(0.0, Epsilon)) {
        if (x.approximateEqual(0.0, Epsilon)) {
            // either 0 or 1, depending on the limit direction
            return Double.NaN
        }
        return 1.0
    }

    if (x.approximateEqual(0.0, Epsilon)) {
        return 0.0
    }

    val ax = (a * ln(x)) - x - logGamma(a)
    if (ax < -709.78271289338399) {
        return 1.0
    }

    if (x <= 1 || x <= a) {
        var r2 = a
        var c2 = 1.0
        var ans2 = 1.0

        do {
            r2 += 1
            c2 = c2 * x / r2
            ans2 += c2
        } while ((c2 / ans2) > Epsilon)

        return exp(ax) * ans2 / a
    }

    var c = 0
    var y = 1 - a
    var z = x + y + 1

    var p3 = 1.0
    var q3 = x
    var p2 = x + 1
    var q2 = z * x
    var ans = p2 / q2

    var error: Double

    do {
        c++
        y += 1
        z += 2
        val yc = y * c

        val p = (p2 * z) - (p3 * yc)
        val q = (q2 * z) - (q3 * yc)

        if (abs(q - 0) > Epsilon) {
            val nextans = p / q
            error = abs((ans - nextans) / nextans)
            ans = nextans
        } else {
            // zero div, skip
            error = 1.0
        }

        // shift
        p3 = p2
        p2 = p
        q3 = q2
        q2 = q

        // normalize fraction when the numerator becomes large
        if (abs(p) > BigNumber) {
            p3 *= BigNumberInverse
            p2 *= BigNumberInverse
            q3 *= BigNumberInverse
            q2 *= BigNumberInverse
        }
    } while (error > Epsilon)

    return 1.0 - exp(ax) * ans
}

//
// GammaLn 
//

/**
 * GammaLn function
 */
fun DoubleArray.gammaLn(): DoubleArray = DoubleArray(size) { logGamma(this@gammaLn[it]) }

/**
 * GammaLn function
 */
fun Iterable<Double>.gammaLn(): List<Double> = map { logGamma(it) }

/**
 * GammaLn function
 */
inline fun <T> Iterable<T>.gammaLn(selector: (T) -> Double): List<Double> = map { logGamma(selector(it)) }

//
// Gamma
//

/**
 * Gamma function
 */
fun DoubleArray.gamma(): DoubleArray = DoubleArray(size) { gamma(this@gamma[it]) } // map { gamma(it) }

/**
 * Gamma function
 */
fun Iterable<Double>.gamma(): List<Double> = map { gamma(it) }

/**
 * Gamma function
 */
inline fun <T> Iterable<T>.gamma(selector: (T) -> Double): List<Double> = map { gamma(selector(it)) }
