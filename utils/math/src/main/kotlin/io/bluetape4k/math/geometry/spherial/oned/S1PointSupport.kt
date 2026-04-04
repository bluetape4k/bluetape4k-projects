package io.bluetape4k.math.geometry.spherial.oned

import org.apache.commons.math3.geometry.spherical.oned.S1Point

/**
 * 숫자를 구면 1D 공간의 점(S1Point)으로 변환합니다.
 *
 * ```kotlin
 * val point = Math.PI.toS1Point()   // S1Point(Math.PI)
 * ```
 */
fun <T: Number> T.toS1Point(): S1Point = S1Point(this.toDouble())
