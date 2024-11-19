package io.bluetape4k.math.geometry.spherial.twod

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.geometry.spherical.twod.Circle
import org.apache.commons.math3.geometry.spherical.twod.S2Point

fun circleOf(pole: Vector3D, tolerance: Double): Circle =
    Circle(pole, tolerance)

fun circlrOf(first: S2Point, second: S2Point, tolerance: Double): Circle =
    Circle(first, second, tolerance)

fun Circle.copy(): Circle = Circle(this)
