package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.oned.Euclidean1D
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D

operator fun Vector1D.plus(v: Vector<Euclidean1D>): Vector1D = this.add(v)

operator fun Vector1D.minus(v: Vector<Euclidean1D>): Vector1D = this.subtract(v)


fun <T: Number> T.toVector1D(): Vector1D = Vector1D(this.toDouble())

fun vector1DOf(a: Double, u: Vector1D): Vector1D =
    Vector1D(a, u)

fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2)

fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
    a3: Double, u3: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2, a3, u3)

fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
    a3: Double, u3: Vector1D,
    a4: Double, u4: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2, a3, u3, a4, u4)
