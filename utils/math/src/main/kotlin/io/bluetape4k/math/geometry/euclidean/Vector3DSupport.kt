package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.threed.Euclidean3D
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

fun DoubleArray.toVector3D(): Vector3D = Vector3D(this)

fun vector3DOf(x: Double, y: Double, z: Double): Vector3D = Vector3D(x, y, z)

fun vector3DOf(alpha: Double, delta: Double): Vector3D = Vector3D(alpha, delta)

fun vector3DOf(a: Double, u: Vector3D): Vector3D =
    Vector3D(a, u)

fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2)

fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
    a3: Double, u3: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2, a3, u3)

fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
    a3: Double, u3: Vector3D,
    a4: Double, u4: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2, a3, u3, a4, u4)


operator fun Vector3D.plus(v: Vector<Euclidean3D>): Vector3D = this.add(v)

operator fun Vector3D.minus(v: Vector<Euclidean3D>): Vector3D = this.subtract(v)

fun Vector3D.angle(that: Vector3D): Double = Vector3D.angle(this, that)
