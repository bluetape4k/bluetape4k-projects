package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.twod.Euclidean2D
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

fun DoubleArray.toVector2D(): Vector2D = Vector2D(this)
fun vector2DOf(x: Double, y: Double): Vector2D = Vector2D(x, y)


operator fun Vector2D.plus(v: Vector<Euclidean2D>): Vector2D = this.add(v)
operator fun Vector2D.minus(v: Vector<Euclidean2D>): Vector2D = this.subtract(v)

operator fun Double.times(v: Vector2D): Vector2D = Vector2D(this, v)

fun Vector2D.angle(v: Vector2D): Double = Vector2D.angle(this, v)
