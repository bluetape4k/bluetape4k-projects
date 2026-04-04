package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheLoessInterpolator = org.apache.commons.math3.analysis.interpolation.LoessInterpolator

/**
 * LOESS(Locally Weighted Scatterplot Smoothing) 보간법으로 데이터를 스무딩합니다.
 * 노이즈가 있는 데이터를 부드럽게 처리하는 데 적합합니다.
 *
 * ```kotlin
 * val interpolator = LoessInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0),
 *     ys = doubleArrayOf(0.1, 0.9, 3.9, 9.1, 16.1)
 * )
 * val y = fn(2.0)   // ≈ 4.0 (스무딩된 보간 값)
 * ```
 */
class LoessInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheLoessInterpolator()

}
