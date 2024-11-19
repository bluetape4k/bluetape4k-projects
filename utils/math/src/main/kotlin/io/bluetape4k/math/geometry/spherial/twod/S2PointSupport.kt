package io.bluetape4k.math.geometry.spherial.twod

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.geometry.spherical.twod.S2Point

fun s2PointOf(theta: Double, phi: Double): S2Point = S2Point(theta, phi)

fun Vector3D.toS2Point(): S2Point = S2Point(this)
