package io.bluetape4k.math

import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.sqrt

object MathConsts {

    const val EPSILON: Double = 1.0e-20
    const val FLOAT_EPSILON: Float = 1e-10F
    val BIGDECIMAL_EPSILON = EPSILON.toBigDecimal()

    const val Pi: Double = Math.PI
    const val Pi2: Double = Pi * 2.0
    const val PiOver2: Double = Pi / 2.0
    const val PiSquare: Double = Pi * Pi

    val sqrtPi: Double get() = sqrt(Pi)
    val sqrtPi2: Double get() = sqrt(Pi * 2.0)
    val sqrtPi2E: Double get() = sqrt(Pi2 * Math.E)
    val lnSqrtPi2: Double get() = ln(sqrtPi2)
    val lnSqrtPi2E: Double get() = ln(sqrtPi2E)
    val ln2SqrtEOverPi: Double get() = ln(twoSqrtEOverPi)
    val invSqrtPi: Double get() = 1.0 / sqrtPi

    val twoSqrtEOverPi: Double get() = 2.0 * sqrt(Math.E / Pi)

    val log2E: Double get() = log(Math.E, 2.0)
    val log10E: Double get() = log10(Math.E)
    val ln2: Double get() = ln(2.0)
    val ln10: Double get() = ln(10.0)
    val lnPi2: Double get() = ln(Pi2)
    val lnPi: Double get() = ln(Pi)

    // 각도 (degree 를 radian으로 변환하기 위한 factor) (Pi / 180)
    val degree: Double = Pi / 180.0

    /**
     * ln(10) / 20 - Power Decibel (dB) 를 Neper (Np) 로 변환할 때의 factor
     * Use this version when the Decibel represent a power gain
     * but the compared values are not powers (e.g. amplitude, current, voltage).
     */
    val powerDecibel: Double get() = ln(10.0) / 20.0

    /**
     * ln(10) / 10 - Neutral Decibel (dB)를 Neper (Np)로 변환할 때의 factor
     * Use this version when the Decibel represent a power gain
     * but the compared values are not powers (e.g. amplitude, current, voltage).
     */
    val neutralDecibel: Double get() = ln(10.0) / 10.0

    /**
     * 황금비 (Golden Ratio) (1+sqrt(5))/2
     */
    val goldenRatio: Double = (1.0 + sqrt(5.0)) / 2.0

    /**
     * The Catalan constant
     * `Sum(k=0 -> inf){ (-1)^k/(2*k + 1)2 }`
     */
    const val CATALAN: Double = 0.9159655941772190150546035149323841107741493742816721342664981196217630197762547694794

    /**
     * The Euler-Mascheroni constant
     * `lim(n -> inf){ Sum(k=1 -> n) { 1/k - log(n) } }`
     */
    const val EULER_MASCHERONI = 0.5772156649015328606065120900824024310421593359399235988057672348849

    /**
     * The Glaisher constant
     * `e^(1/12 - Zeta(-1))`
     */
    const val GLAISHER = 1.2824271291006226368753425688697917277676889273250011920637400217404063088588264611297

    /**
     * The Khinchin constant
     * `prod(k=1 -> inf){1+1/(k*(k+2))^log(k,2)}`</remarks>`
     */
    const val KHINCHIN = 2.6854520010653064453097148354817956938203822939944629530511523455572188595371520028011

    /**
     * 해를 찾기 위한 기본 시되 횟수 (100회)
     */
    const val DefaultTryCount: Int = 10000

    /**
     * 블록의 기본 크기 (구간을 이용한 이동평균이나 합을 구할때 사용)
     */
    const val BLOCK_SIZE: Int = 4

    /**
     * 2의 제곱의 배열
     */
    val POW2: IntArray = intArrayOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768)

    fun pow2(index: Int): Int = POW2[index]
}
