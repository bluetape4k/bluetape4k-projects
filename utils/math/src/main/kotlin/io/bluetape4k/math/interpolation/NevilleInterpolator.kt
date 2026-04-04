package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheNevilleInterpolator = org.apache.commons.math3.analysis.interpolation.NevilleInterpolator

/**
 * Neville 알고리즘으로 다항식 보간을 수행합니다.
 * 주어진 점을 정확히 통과하는 다항식을 생성합니다.
 *
 * ```kotlin
 * val interpolator = NevilleInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0),
 *     ys = doubleArrayOf(0.0, 1.0, 4.0, 9.0, 16.0)
 * )
 * val y = fn(1.5)   // ≈ 2.25 (다항식 보간 값)
 * ```
 */
class NevilleInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheNevilleInterpolator()

}
