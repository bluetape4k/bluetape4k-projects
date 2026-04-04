package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheSplineInterpolator = org.apache.commons.math3.analysis.interpolation.SplineInterpolator

/**
 * 3차 스플라인 보간법으로 데이터 포인트를 부드럽게 연결합니다.
 * 각 구간에서 3차 다항식을 사용하며, 경계에서 1·2차 미분값이 연속됩니다.
 *
 * ```kotlin
 * val interpolator = SplineInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0),
 *     ys = doubleArrayOf(0.0, 1.0, 4.0, 9.0, 16.0)
 * )
 * val y = fn(1.5)   // ≈ 2.25 (스플라인 보간 값)
 * ```
 */
class SplineInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheSplineInterpolator()

}
