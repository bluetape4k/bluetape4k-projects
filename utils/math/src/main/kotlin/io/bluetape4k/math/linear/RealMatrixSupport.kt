package io.bluetape4k.math.linear

import org.apache.commons.math3.linear.AnyMatrix
import org.apache.commons.math3.linear.RealMatrix

/**
 * 두 실수 행렬을 더합니다.
 *
 * ```kotlin
 * val m1 = realMatrixOf(2, 2)
 * val m2 = realMatrixOf(2, 2)
 * val result = m1 + m2
 * ```
 */
operator fun RealMatrix.plus(rm: RealMatrix): RealMatrix = add(rm)

/**
 * 실수 행렬에 스칼라를 더합니다.
 *
 * ```kotlin
 * val m = realMatrixOf(2, 2)
 * val result = m + 1.0
 * ```
 */
operator fun <N: Number> RealMatrix.plus(scalar: N): RealMatrix = scalarAdd(scalar.toDouble())

/**
 * 두 실수 행렬을 뺍니다.
 *
 * ```kotlin
 * val m1 = realMatrixOf(2, 2)
 * val m2 = realMatrixOf(2, 2)
 * val result = m1 - m2
 * ```
 */
operator fun RealMatrix.minus(rm: RealMatrix): RealMatrix = subtract(rm)

/**
 * 실수 행렬에서 스칼라를 뺍니다.
 *
 * ```kotlin
 * val m = realMatrixOf(2, 2)
 * val result = m - 1.0
 * ```
 */
operator fun <N: Number> RealMatrix.minus(scalar: N): RealMatrix = scalarAdd(-scalar.toDouble())

/**
 * 두 실수 행렬을 곱합니다.
 *
 * ```kotlin
 * val m1 = realMatrixOf(2, 3)
 * val m2 = realMatrixOf(3, 2)
 * val result = m1 * m2   // 2x2 행렬
 * ```
 */
operator fun RealMatrix.times(rm: RealMatrix): RealMatrix = multiply(rm)

/**
 * 실수 행렬에 스칼라를 곱합니다.
 *
 * ```kotlin
 * val m = realMatrixOf(2, 2)
 * val result = m * 2.0
 * ```
 */
operator fun <N: Number> RealMatrix.times(scalar: N): AnyMatrix = scalarMultiply(scalar.toDouble())

/**
 * 실수 행렬을 다른 행렬로 나눕니다 (역행렬을 곱합니다).
 *
 * ```kotlin
 * val m1 = realIdentityMatrixOf(2)
 * val m2 = realIdentityMatrixOf(2)
 * val result = m1 / m2   // 단위행렬
 * ```
 */
operator fun RealMatrix.div(rm: RealMatrix): RealMatrix = multiply(rm.inverse())

/**
 * 실수 행렬을 스칼라로 나눕니다.
 *
 * ```kotlin
 * val m = realMatrixOf(2, 2)
 * val result = m / 2.0
 * ```
 */
operator fun <N: Number> RealMatrix.div(scalar: N): AnyMatrix = scalarMultiply(1.0 / scalar.toDouble())
