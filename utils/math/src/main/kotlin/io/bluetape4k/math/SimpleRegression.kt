@file:JvmName("simpleregression")

package io.bluetape4k.math

/**
 * 단순 선형 회귀(Simple Linear Regression) 결과를 제공하는 인터페이스.
 * y = slope * x + intercept 형태의 회귀 모델을 나타냅니다.
 *
 * ```kotlin
 * val data = listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0)
 * val reg = data.simpleRegression()
 * val predicted = reg.predict(4.0)   // 8.0 (y = 2x + 0)
 * ```
 */
interface SimpleRegression {
    /** 데이터 포인트 수 */
    val n: Long

    /** 회귀선의 y절편 */
    val intercept: Double

    /** 회귀선의 기울기 */
    val slope: Double

    /** 오차 제곱합 (Sum of Squared Errors) */
    val sumSquaredErrors: Double

    /** 전체 제곱합 (Total Sum of Squares) */
    val totalSumSqaures: Double

    /** X의 제곱합 */
    val xSumSquares: Double

    /** X와 Y의 교차곱 합 */
    val sumOfCrossProducts: Double

    /** 회귀 제곱합 (Regression Sum of Squares) */
    val regressionSumSquares: Double

    /** 평균 제곱 오차 (Mean Square Error) */
    val meanSquareError: Double

    /** 피어슨 상관계수 */
    val r: Double

    /** 결정계수 (R²) */
    val rSquare: Double

    /** y절편의 표준 오차 */
    val intereptStdErr: Double

    /** 기울기의 표준 오차 */
    val slopeStdErr: Double

    /** 기울기의 신뢰 구간 */
    val slopeConfidenceInterval: Double

    /** 기울기의 유의성 (p-value) */
    val significance: Double

    /**
     * 주어진 x값에 대한 예측 y값을 반환합니다.
     *
     * ```kotlin
     * val data = listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0)
     * val reg = data.simpleRegression()
     * val y = reg.predict(4.0)   // 8.0
     * ```
     */
    fun predict(x: Double): Double
}


/**
 * Apache Commons Math의 [org.apache.commons.math3.stat.regression.SimpleRegression]을 감싸는 구현체.
 *
 * ```kotlin
 * val data = listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0)
 * val reg = data.simpleRegression()
 * val y = reg.predict(5.0)   // 10.0
 * ```
 */
class ApacheSimpleRegression(val sr: org.apache.commons.math3.stat.regression.SimpleRegression): SimpleRegression {
    override val n get() = sr.n
    override val intercept get() = sr.intercept
    override val slope get() = sr.slope
    override val sumSquaredErrors get() = sr.sumSquaredErrors
    override val totalSumSqaures get() = sr.totalSumSquares
    override val xSumSquares get() = sr.xSumSquares
    override val sumOfCrossProducts get() = sr.sumOfCrossProducts
    override val regressionSumSquares get() = sr.regressionSumSquares
    override val meanSquareError get() = sr.meanSquareError
    override val r get() = sr.r
    override val rSquare get() = sr.rSquare
    override val intereptStdErr get() = sr.interceptStdErr
    override val slopeStdErr get() = sr.slopeStdErr
    override val slopeConfidenceInterval get() = sr.slopeConfidenceInterval
    override val significance get() = sr.significance
    override fun predict(x: Double) = sr.predict(x)
}
