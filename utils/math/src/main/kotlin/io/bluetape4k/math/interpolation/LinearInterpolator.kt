package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheLinearInterpolator = org.apache.commons.math3.analysis.interpolation.LinearInterpolator

/**
 * 선형 보간법으로 데이터 포인트를 보간합니다.
 * 인접한 두 점 사이를 직선으로 연결하여 중간 값을 계산합니다.
 *
 * ```kotlin
 * val interpolator = LinearInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0),
 *     ys = doubleArrayOf(0.0, 2.0, 4.0)
 * )
 * val y = fn(0.5)   // 1.0 (선형 보간)
 * ```
 */
class LinearInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheLinearInterpolator()

}
