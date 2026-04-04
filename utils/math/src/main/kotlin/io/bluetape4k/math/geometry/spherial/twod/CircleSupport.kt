package io.bluetape4k.math.geometry.spherial.twod

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.geometry.spherical.twod.Circle
import org.apache.commons.math3.geometry.spherical.twod.S2Point

/**
 * 극 벡터(pole)로 구면 2D 원(Circle)을 생성합니다.
 *
 * ```kotlin
 * val pole = Vector3D(0.0, 0.0, 1.0)
 * val circle = circleOf(pole = pole, tolerance = 1e-10)
 * ```
 */
fun circleOf(pole: Vector3D, tolerance: Double): Circle =
    Circle(pole, tolerance)

/**
 * 두 구면 점(S2Point)을 지나는 대원(great circle)을 생성합니다.
 *
 * ```kotlin
 * val p1 = s2PointOf(0.0, 0.0)
 * val p2 = s2PointOf(Math.PI / 2, 0.0)
 * val circle = circlrOf(first = p1, second = p2, tolerance = 1e-10)
 * ```
 */
fun circlrOf(first: S2Point, second: S2Point, tolerance: Double): Circle =
    Circle(first, second, tolerance)

/**
 * 기존 원(Circle)을 복사하여 새로운 Circle을 반환합니다.
 *
 * ```kotlin
 * val circle = circleOf(Vector3D(0.0, 0.0, 1.0), 1e-10)
 * val copy = circle.copy()
 * ```
 */
fun Circle.copy(): Circle = Circle(this)
