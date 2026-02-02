package io.bluetape4k.math.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.interpolation.Interpolator
import io.bluetape4k.math.interpolation.LinearInterpolator
import io.bluetape4k.support.assertPositiveNumber

/**
 * 적분 (Integrator) 을 수행합니다.
 */
interface Integrator {

    companion object: KLogging() {
        const val DEFAULT_MAXEVAL: Int = 1000
        val DefaultInterpolator = LinearInterpolator()
    }

    val relativeAccuracy: Double
    val absoluteAccuracy: Double

    /**
     * 함수의 [a, b] 구간을 적분합니다.
     *
     * @param evaluator 적분할 함수
     * @param lower 시작 위치
     * @param upper 끝 위치
     * @return 적분 값
     */
    fun integrate(lower: Double, upper: Double, evaluator: (Double) -> Double): Double

    fun integrate(
        xs: DoubleArray,
        ys: DoubleArray,
        interpolator: Interpolator = DefaultInterpolator,
    ): Double {
        assert(xs.isNotEmpty()) { "xs must not be empty." }
        assert(ys.isNotEmpty()) { "ys must not be empty." }
        assert(xs.count() == ys.count()) { "xs size must same with ys size" }

        val evaluator = interpolator.interpolate(xs, ys)
        return integrate(xs.first(), xs.last(), evaluator)
    }

    fun integrate(
        xy: Iterable<Pair<Double, Double>>,
        interpolator: Interpolator = DefaultInterpolator,
    ): Double {
        val count = xy.count()
        count.assertPositiveNumber("collection must have elements.")

        val xs = DoubleArray(count)
        val ys = DoubleArray(count)

        xy.forEachIndexed { i, (x, y) ->
            xs[i] = x
            ys[i] = y
        }

        return integrate(xs, ys, interpolator)
    }
}
