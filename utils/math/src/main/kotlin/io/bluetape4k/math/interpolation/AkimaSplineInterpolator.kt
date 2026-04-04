package io.bluetape4k.math.interpolation

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator

typealias ApacheAkimaSplineInterpolator = org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator

/**
 * Akima 스플라인 보간법으로 데이터 포인트를 보간합니다.
 * 진동이 적고 자연스러운 곡선을 생성하며, 최소 5개의 데이터 포인트가 필요합니다.
 *
 * ```kotlin
 * val interpolator = AkimaSplineInterpolator()
 * val fn = interpolator.interpolate(
 *     xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0),
 *     ys = doubleArrayOf(0.0, 1.0, 4.0, 9.0, 16.0)
 * )
 * val y = fn(1.5)   // ≈ 2.25 (보간된 값)
 * ```
 */
class AkimaSplineInterpolator: AbstractInterpolator() {

    override val apacheInterpolator: UnivariateInterpolator = ApacheAkimaSplineInterpolator()
}
