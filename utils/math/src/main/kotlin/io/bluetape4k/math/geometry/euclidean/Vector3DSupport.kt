package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.threed.Euclidean3D
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D

/**
 * Double 배열로부터 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val v = doubleArrayOf(1.0, 2.0, 3.0).toVector3D()   // Vector3D(1.0, 2.0, 3.0)
 * ```
 */
fun DoubleArray.toVector3D(): Vector3D = Vector3D(this)

/**
 * x, y, z 좌표로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val v = vector3DOf(1.0, 2.0, 3.0)   // Vector3D(1.0, 2.0, 3.0)
 * ```
 */
fun vector3DOf(x: Double, y: Double, z: Double): Vector3D = Vector3D(x, y, z)

/**
 * 구면 좌표(azimuthal angle, polar angle)로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val v = vector3DOf(alpha = 0.0, delta = 0.0)   // Vector3D(1.0, 0.0, 0.0)
 * ```
 */
fun vector3DOf(alpha: Double, delta: Double): Vector3D = Vector3D(alpha, delta)

/**
 * 스칼라 `a`와 벡터 `u`의 선형 결합으로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u = vector3DOf(1.0, 0.0, 0.0)
 * val v = vector3DOf(2.0, u)   // Vector3D(2.0, 0.0, 0.0)
 * ```
 */
fun vector3DOf(a: Double, u: Vector3D): Vector3D =
    Vector3D(a, u)

/**
 * 두 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u1 = vector3DOf(1.0, 0.0, 0.0)
 * val u2 = vector3DOf(0.0, 1.0, 0.0)
 * val v = vector3DOf(2.0, u1, 3.0, u2)   // Vector3D(2.0, 3.0, 0.0)
 * ```
 */
fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2)

/**
 * 세 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u1 = Vector3D.PLUS_I; val u2 = Vector3D.PLUS_J; val u3 = Vector3D.PLUS_K
 * val v = vector3DOf(1.0, u1, 2.0, u2, 3.0, u3)   // Vector3D(1.0, 2.0, 3.0)
 * ```
 */
fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
    a3: Double, u3: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2, a3, u3)

/**
 * 네 스칼라-벡터 쌍의 선형 결합으로 3차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u = Vector3D.PLUS_I
 * val v = vector3DOf(1.0, u, 2.0, u, 3.0, u, 4.0, u)   // Vector3D(10.0, 0.0, 0.0)
 * ```
 */
fun vector3DOf(
    a1: Double, u1: Vector3D,
    a2: Double, u2: Vector3D,
    a3: Double, u3: Vector3D,
    a4: Double, u4: Vector3D,
): Vector3D =
    Vector3D(a1, u1, a2, u2, a3, u3, a4, u4)

/**
 * 두 3차원 벡터를 더합니다.
 *
 * ```kotlin
 * val v1 = vector3DOf(1.0, 2.0, 3.0)
 * val v2 = vector3DOf(4.0, 5.0, 6.0)
 * val result = v1 + v2   // Vector3D(5.0, 7.0, 9.0)
 * ```
 */
operator fun Vector3D.plus(v: Vector<Euclidean3D>): Vector3D = this.add(v)

/**
 * 두 3차원 벡터를 뺍니다.
 *
 * ```kotlin
 * val v1 = vector3DOf(5.0, 7.0, 9.0)
 * val v2 = vector3DOf(1.0, 2.0, 3.0)
 * val result = v1 - v2   // Vector3D(4.0, 5.0, 6.0)
 * ```
 */
operator fun Vector3D.minus(v: Vector<Euclidean3D>): Vector3D = this.subtract(v)

/**
 * 두 3차원 벡터 사이의 각도(라디안)를 계산합니다.
 *
 * ```kotlin
 * val v1 = vector3DOf(1.0, 0.0, 0.0)
 * val v2 = vector3DOf(0.0, 1.0, 0.0)
 * val angle = v1.angle(v2)   // Math.PI / 2 (90도)
 * ```
 */
fun Vector3D.angle(that: Vector3D): Double = Vector3D.angle(this, that)
