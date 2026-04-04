package io.bluetape4k.math.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.interpolation.Interpolator
import io.bluetape4k.math.interpolation.LinearInterpolator
import io.bluetape4k.support.assertPositiveNumber

/**
 * 적분 (Integrator) 을 수행합니다.
 *
 * ```kotlin
 * val integrator = SimpsonIntegrator()
 * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
 * // result ≈ 0.3333 (∫x² dx from 0 to 1)
 * ```
 */
interface Integrator {

    companion object: KLogging() {
        const val DEFAULT_MAXEVAL: Int = 1000
        val DefaultInterpolator = LinearInterpolator()
    }

    val relativeAccuracy: Double
    val absoluteAccuracy: Double

    /**
     * 함수의 [lower, upper] 구간을 적분합니다.
     *
     * ```kotlin
     * val integrator = SimpsonIntegrator()
     * val result = integrator.integrate(lower = 0.0, upper = 1.0) { x -> x * x }
     * // result ≈ 0.3333 (∫x² dx from 0 to 1)
     * ```
     *
     * @param evaluator 적분할 함수
     * @param lower 시작 위치
     * @param upper 끝 위치
     * @return 적분 값
     */
    fun integrate(lower: Double, upper: Double, evaluator: (Double) -> Double): Double

    /**
     * 데이터 배열로부터 보간 함수를 생성하여 적분합니다.
     *
     * ```kotlin
     * val integrator = SimpsonIntegrator()
     * val result = integrator.integrate(
     *     xs = doubleArrayOf(0.0, 0.5, 1.0),
     *     ys = doubleArrayOf(0.0, 0.25, 1.0)
     * )
     * // result ≈ 0.3333 (선형 보간 후 적분)
     * ```
     */
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

    /**
     * (x, y) 쌍의 컬렉션으로부터 보간하여 적분합니다.
     *
     * ```kotlin
     * val integrator = SimpsonIntegrator()
     * val result = integrator.integrate(
     *     xy = listOf(0.0 to 0.0, 0.5 to 0.25, 1.0 to 1.0)
     * )
     * // result ≈ 0.3333 (보간 후 적분)
     * ```
     */
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
