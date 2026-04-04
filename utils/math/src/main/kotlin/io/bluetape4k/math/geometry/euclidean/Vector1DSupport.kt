package io.bluetape4k.math.geometry.euclidean

import org.apache.commons.math3.geometry.Vector
import org.apache.commons.math3.geometry.euclidean.oned.Euclidean1D
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D

/**
 * 두 1차원 벡터를 더합니다.
 *
 * ```kotlin
 * val v1 = Vector1D(1.0)
 * val v2 = Vector1D(2.0)
 * val result = v1 + v2   // Vector1D(3.0)
 * ```
 */
operator fun Vector1D.plus(v: Vector<Euclidean1D>): Vector1D = this.add(v)

/**
 * 두 1차원 벡터를 뺍니다.
 *
 * ```kotlin
 * val v1 = Vector1D(5.0)
 * val v2 = Vector1D(3.0)
 * val result = v1 - v2   // Vector1D(2.0)
 * ```
 */
operator fun Vector1D.minus(v: Vector<Euclidean1D>): Vector1D = this.subtract(v)

/**
 * 숫자를 1차원 벡터로 변환합니다.
 *
 * ```kotlin
 * val v = 3.14.toVector1D()   // Vector1D(3.14)
 * ```
 */
fun <T: Number> T.toVector1D(): Vector1D = Vector1D(this.toDouble())

/**
 * 스칼라 `a`와 벡터 `u`의 선형 결합으로 1차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u = Vector1D(2.0)
 * val v = vector1DOf(3.0, u)   // Vector1D(6.0)
 * ```
 */
fun vector1DOf(a: Double, u: Vector1D): Vector1D =
    Vector1D(a, u)

/**
 * 두 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u1 = Vector1D(1.0)
 * val u2 = Vector1D(2.0)
 * val v = vector1DOf(2.0, u1, 3.0, u2)   // Vector1D(2*1 + 3*2) = Vector1D(8.0)
 * ```
 */
fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2)

/**
 * 세 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u1 = Vector1D(1.0); val u2 = Vector1D(2.0); val u3 = Vector1D(3.0)
 * val v = vector1DOf(1.0, u1, 2.0, u2, 3.0, u3)   // Vector1D(1+4+9) = Vector1D(14.0)
 * ```
 */
fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
    a3: Double, u3: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2, a3, u3)

/**
 * 네 스칼라-벡터 쌍의 선형 결합으로 1차원 벡터를 생성합니다.
 *
 * ```kotlin
 * val u1 = Vector1D(1.0); val u2 = Vector1D(1.0)
 * val u3 = Vector1D(1.0); val u4 = Vector1D(1.0)
 * val v = vector1DOf(1.0, u1, 2.0, u2, 3.0, u3, 4.0, u4)   // Vector1D(10.0)
 * ```
 */
fun vector1DOf(
    a1: Double, u1: Vector1D,
    a2: Double, u2: Vector1D,
    a3: Double, u3: Vector1D,
    a4: Double, u4: Vector1D,
): Vector1D =
    Vector1D(a1, u1, a2, u2, a3, u3, a4, u4)
