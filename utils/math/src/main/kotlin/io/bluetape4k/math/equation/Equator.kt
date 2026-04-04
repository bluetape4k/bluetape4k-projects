package io.bluetape4k.math.equation

import io.bluetape4k.math.commons.minMax
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator

/**
 * 근의 공식을 사용한다.
 *
 * 방정식의 근(root)을 수치적으로 찾는 공통 인터페이스입니다.
 * 구체적인 알고리즘은 구현 클래스에서 제공합니다.
 *
 * ```kotlin
 * val equator = BisectionEquator()
 * val root = equator.solve(min = -1.0, max = 1.0) { x -> x * x - 0.5 }
 * // root ≈ 0.7071 (sqrt(0.5))
 * ```
 */
interface Equator {

    companion object {
        const val MAXEVAL: Int = 100
    }

    /** 절대 정확도 (수렴 판단 기준) */
    val absoluteAccuracy: Double

    /**
     * 함수 `evaluator`의 `[min, max]` 구간에서 근을 찾습니다.
     *
     * ```kotlin
     * val root = equator.solve(min = 0.0, max = 1.0) { x -> x - 0.5 }
     * // root == 0.5
     * ```
     */
    fun solve(maxEval: Int = MAXEVAL, min: Double, max: Double, evaluator: (Double) -> Double): Double

    /**
     * 보간을 통해 (xs, ys) 데이터셋에서 근을 찾습니다.
     *
     * ```kotlin
     * val root = equator.solve(xs = doubleArrayOf(0.0, 1.0), ys = doubleArrayOf(-1.0, 1.0))
     * // root ≈ 0.5 (선형 보간)
     * ```
     */
    fun solve(maxEval: Int = MAXEVAL, xs: DoubleArray, ys: DoubleArray): Double {
        val (min, max) = xs.minMax()
        val interpolator = LinearInterpolator()
        val evaluator = interpolator.interpolate(xs, ys)

        return solve(maxEval, min, max) { evaluator.value(it) }
    }

    fun solve(maxEval: Int = MAXEVAL, values: Iterable<Pair<Double, Double>>): Double {
        val size = values.count()
        val xs = DoubleArray(size)
        val ys = DoubleArray(size)

        values.forEachIndexed { i, (x, y) ->
            xs[i] = x
            ys[i] = y
        }

        return solve(maxEval, xs, ys)
    }
}
