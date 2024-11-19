package io.bluetape4k.math.equation

import io.bluetape4k.math.commons.minMax
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator

/**
 * 근의 공식을 사용한다.
 */
interface Equator {

    companion object {
        const val MAXEVAL: Int = 100
    }

    val absoluteAccuracy: Double

    fun solve(maxEval: Int = MAXEVAL, min: Double, max: Double, evaluator: (Double) -> Double): Double

    fun solve(maxEval: Int = MAXEVAL, xs: DoubleArray, ys: DoubleArray): Double {
        val (min, max) = xs.minMax()
        val interpolator = LinearInterpolator()
        val evaluator = interpolator.interpolate(xs, ys)

        return solve(maxEval, min, max) { evaluator.value(it) }
    }

    fun solve(maxEval: Int = MAXEVAL, values: Collection<Pair<Double, Double>>): Double {
        val size = values.size
        val xs = DoubleArray(size)
        val ys = DoubleArray(size)

        values.forEachIndexed { i, (x, y) ->
            xs[i] = x
            ys[i] = y
        }
        return solve(maxEval, xs, ys)
    }
}
