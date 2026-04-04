package io.bluetape4k.math.special

import org.apache.commons.math3.special.BesselJ
import org.apache.commons.math3.special.Beta
import org.apache.commons.math3.special.Erf
import org.apache.commons.math3.special.Gamma

private const val DEFAULT_EPSILON: Double = 1.0e-14

/**
 * Returns the first Bessel function, \(J_{order}(x)\).
 *
 * ```kotlin
 * val result = 0.0.besselj(0.0)   // 1.0 (J_0(0) = 1)
 * ```
 *
 * @param order Order of the Bessel function
 * @return Value of Bessel function of the first kind.
 */
fun Double.besselj(order: Double) = BesselJ.value(order, this)

/**
 * 정규화되지 않은 로그 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = 1.0.logBeta(1.0)   // 0.0
 * ```
 */
fun Double.logBeta(q: Double) = Beta.logBeta(this, q)

/**
 * 정규화 불완전 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = regularizedBeta(0.5, 1.0, 1.0)   // 0.5
 * ```
 */
fun regularizedBeta(
    x: Double,
    a: Double,
    b: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double =
    Beta.regularizedBeta(x, a, b, epsilon, maxIterations)

/**
 * 오차 함수(error function)를 계산합니다.
 *
 * ```kotlin
 * val result = 0.0.erf()   // 0.0
 * val result2 = 1.0.erf()  // 0.8427...
 * ```
 */
fun Double.erf(): Double = Erf.erf(this)

/**
 * `[this, x2]` 구간의 오차 함수를 계산합니다.
 *
 * ```kotlin
 * val result = 0.0.erf(1.0)   // erf(1.0) - erf(0.0) ≈ 0.8427
 * ```
 */
fun Double.erf(x2: Double): Double = Erf.erf(this, x2)

/**
 * 역 오차 함수를 계산합니다.
 *
 * ```kotlin
 * val result = 0.8427.erfInv()   // 약 1.0
 * ```
 */
fun Double.erfInv(): Double = Erf.erfInv(this)

/**
 * 여 오차 함수(complementary error function)를 계산합니다. `1 - erf(x)`
 *
 * ```kotlin
 * val result = 0.0.erfc()   // 1.0
 * ```
 */
fun Double.erfc() = Erf.erfc(this)

/**
 * 역 여 오차 함수를 계산합니다.
 *
 * ```kotlin
 * val result = 1.0.erfcInv()   // 0.0
 * ```
 */
fun Double.erfcInv() = Erf.erfcInv(this)

/**
 * 감마 함수를 계산합니다.
 *
 * ```kotlin
 * val result = 1.0.gamma()   // 1.0 (Gamma(1) = 1)
 * val result2 = 5.0.gamma()  // 24.0 (Gamma(5) = 4!)
 * ```
 */
fun Double.gamma() = Gamma.gamma(this)

/**
 * 디감마 함수(감마 함수의 로그 미분)를 계산합니다.
 *
 * ```kotlin
 * val result = 1.0.digamma()   // -0.5772... (Euler-Mascheroni 상수)
 * ```
 */
fun Double.digamma() = Gamma.digamma(this)

/**
 * 트리감마 함수(디감마 함수의 미분)를 계산합니다.
 *
 * ```kotlin
 * val result = 1.0.trigamma()   // pi^2/6 ≈ 1.6449
 * ```
 */
fun Double.trigamma() = Gamma.trigamma(this)

/**
 * 감마 함수의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = 5.0.logGamma()   // ln(24) ≈ 3.178
 * ```
 */
fun Double.logGamma() = Gamma.logGamma(this)

/**
 * `Gamma(1 + this)`의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = 4.0.logGamma1p()   // ln(Gamma(5)) = ln(24) ≈ 3.178
 * ```
 */
fun Double.logGamma1p() = Gamma.logGamma1p(this)

/**
 * 하위 불완전 정규화 감마 함수 `P(a, x)`를 계산합니다.
 *
 * ```kotlin
 * val result = regularizedGammaP(1.0, 1.0)   // 1 - e^-1 ≈ 0.6321
 * ```
 */
fun regularizedGammaP(
    a: Double,
    x: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double {
    return Gamma.regularizedGammaP(a, x, epsilon, maxIterations)
}

/**
 * 상위 불완전 정규화 감마 함수 `Q(a, x) = 1 - P(a, x)`를 계산합니다.
 *
 * ```kotlin
 * val result = regularizedGammaQ(1.0, 1.0)   // e^-1 ≈ 0.3678
 * ```
 */
fun regularizedGammaQ(
    a: Double,
    x: Double,
    epsilon: Double = DEFAULT_EPSILON,
    maxIterations: Int = Int.MAX_VALUE,
): Double {
    return Gamma.regularizedGammaQ(a, x, epsilon, maxIterations)
}
