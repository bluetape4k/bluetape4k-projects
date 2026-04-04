package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.twod.Euclidean2D
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D

/**
 * Double 배열로부터 2차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val v = doubleArrayOf(3.0, 4.0).toVector2D()   // Vector2D(3.0, 4.0)
 * ```
 */
fun DoubleArray.toVector2D(): Vector2D = Vector2D(this)

/**
 * x, y 좌표로 2차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val v = vector2DOf(3.0, 4.0)   // Vector2D(3.0, 4.0)
 * ```
 */
fun vector2DOf(x: Double, y: Double): Vector2D = Vector2D(x, y)

/**
 * 두 2차원 벡터를 더합니다.
 *
 * ```kotlin
 * val v1 = vector2DOf(1.0, 2.0)
 * val v2 = vector2DOf(3.0, 4.0)
 * val result = v1 + v2   // Vector2D(4.0, 6.0)
 * ```
 */
operator fun Vector2D.plus(v: Vector<Euclidean2D>): Vector2D = this.add(v)

/**
 * 두 2차원 벡터를 뺍니다.
 *
 * ```kotlin
 * val v1 = vector2DOf(5.0, 7.0)
 * val v2 = vector2DOf(2.0, 3.0)
 * val result = v1 - v2   // Vector2D(3.0, 4.0)
 * ```
 */
operator fun Vector2D.minus(v: Vector<Euclidean2D>): Vector2D = this.subtract(v)

/**
 * 스칼라와 2차원 벡터를 곱합니다.
 *
 * ```kotlin
 * val v = vector2DOf(1.0, 2.0)
 * val result = 3.0 * v   // Vector2D(3.0, 6.0)
 * ```
 */
operator fun Double.times(v: Vector2D): Vector2D = Vector2D(this, v)

/**
 * 두 2차원 벡터 사이의 각도(라디안)를 계산합니다.
 *
 * ```kotlin
 * val v1 = vector2DOf(1.0, 0.0)
 * val v2 = vector2DOf(0.0, 1.0)
 * val angle = v1.angle(v2)   // Math.PI / 2 (90도)
 * ```
 */
fun Vector2D.angle(v: Vector2D): Double = Vector2D.angle(this, v)
