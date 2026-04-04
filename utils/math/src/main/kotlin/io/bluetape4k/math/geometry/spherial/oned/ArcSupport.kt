package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.spherical.oned.Arc

/**
 * 구면 1D 공간에서 호(Arc)를 생성합니다.
 *
 * ```kotlin
 * val arc = arcOf(lower = 0.0, upper = Math.PI, tolerance = 1e-10)
 * // arc.inf == 0.0, arc.sup == Math.PI
 * ```
 */
fun arcOf(lower: Double, upper: Double, tolerance: Double): Arc =
    Arc(lower, upper, tolerance)
