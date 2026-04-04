package io.bluetape4k.math.geometry.spherial.twod

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.geometry.spherical.twod.S2Point

/**
 * 구면 좌표(theta, phi)로 구면 2D 점(S2Point)을 생성합니다.
 *
 * ```kotlin
 * val point = s2PointOf(theta = 0.0, phi = 0.0)   // 북극 방향 점
 * ```
 */
fun s2PointOf(theta: Double, phi: Double): S2Point = S2Point(theta, phi)

/**
 * 3D 벡터를 구면 2D 점(S2Point)으로 변환합니다.
 *
 * ```kotlin
 * val v = Vector3D(0.0, 0.0, 1.0)
 * val point = v.toS2Point()   // 북극에 해당하는 S2Point
 * ```
 */
fun Vector3D.toS2Point(): S2Point = S2Point(this)
