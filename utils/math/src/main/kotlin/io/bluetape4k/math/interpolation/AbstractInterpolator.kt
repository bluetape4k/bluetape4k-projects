package io.bluetape4k.math.interpolation

import io.bluetape4k.logging.KLogging
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

/**
 * [Interpolator] 인터페이스의 기본 구현체.
 * Apache Commons Math의 [UnivariateInterpolator]를 감싸 Kotlin 람다 기반 API를 제공합니다.
 *
 * ```kotlin
 * // 구체적인 구현 클래스를 통해 사용합니다.
 * val interpolator = SplineInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0),
 *     ys = doubleArrayOf(0.0, 1.0, 4.0, 9.0, 16.0)
 * )
 * val y = fn(1.5)   // ≈ 2.25 (보간된 값)
 * ```
 */
abstract class AbstractInterpolator: Interpolator {

    companion object: KLogging() {
        const val MINIMUM_SIZE = 5
    }

    protected abstract val apacheInterpolator: UnivariateInterpolator

    /**
     * X, Y 변량에 따른 함수를 보간하는 함수를 반환합니다.
     *
     * ```kotlin
     * val interpolator = LinearInterpolator()
     * val fn = interpolator.interpolate(
     *     xs = doubleArrayOf(0.0, 1.0, 2.0),
     *     ys = doubleArrayOf(0.0, 2.0, 4.0)
     * )
     * val y = fn(0.5)   // 1.0 (선형 보간)
     * ```
     *
     * @param xs x 좌표 배열
     * @param ys y 좌표 배열
     * @return 보간 함수 `(Double) -> Double`
     */
    override fun interpolate(xs: DoubleArray, ys: DoubleArray): (Double) -> Double {
        val interpolationFunc = apacheInterpolator.interpolate(xs, ys)
        return { x: Double -> interpolationFunc.value(x) }
    }
}
