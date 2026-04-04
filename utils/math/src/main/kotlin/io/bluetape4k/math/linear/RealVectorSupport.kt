package io.bluetape4k.math.linear

import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.RealVector

/**
 * 인덱스로 벡터 원소를 가져옵니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0))
 * val x = v[0]   // 1.0
 * ```
 */
operator fun RealVector.get(index: Int): Double = getEntry(index)

/**
 * 인덱스로 벡터 원소를 설정합니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0))
 * v[0] = 10.0
 * ```
 */
operator fun RealVector.set(index: Int, value: Double) = setEntry(index, value)

/**
 * 두 실수 벡터를 더합니다.
 *
 * ```kotlin
 * val v1 = realVectorOf(doubleArrayOf(1.0, 2.0))
 * val v2 = realVectorOf(doubleArrayOf(3.0, 4.0))
 * val result = v1 + v2   // [4.0, 6.0]
 * ```
 */
operator fun RealVector.plus(v: RealVector): RealVector = add(v)

/**
 * 실수 벡터에 스칼라를 더합니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0))
 * val result = v + 1.0   // [2.0, 3.0, 4.0]
 * ```
 */
operator fun <N: Number> RealVector.plus(scalar: Number): RealVector = mapAdd(scalar.toDouble())

/**
 * 두 실수 벡터를 뺍니다.
 *
 * ```kotlin
 * val v1 = realVectorOf(doubleArrayOf(5.0, 7.0))
 * val v2 = realVectorOf(doubleArrayOf(1.0, 2.0))
 * val result = v1 - v2   // [4.0, 5.0]
 * ```
 */
operator fun RealVector.minus(v: RealVector): RealVector = subtract(v)

/**
 * 실수 벡터에서 스칼라를 뺍니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(3.0, 5.0, 7.0))
 * val result = v - 2.0   // [1.0, 3.0, 5.0]
 * ```
 */
operator fun <N: Number> RealVector.minus(scalar: Number): RealVector = mapSubtract(scalar.toDouble())

/**
 * 두 실수 벡터를 원소별로 곱합니다 (element-wise multiply).
 *
 * ```kotlin
 * val v1 = realVectorOf(doubleArrayOf(2.0, 3.0))
 * val v2 = realVectorOf(doubleArrayOf(4.0, 5.0))
 * val result = v1 * v2   // [8.0, 15.0]
 * ```
 */
operator fun RealVector.times(v: RealVector): RealVector = ebeMultiply(v)

/**
 * 실수 벡터에 스칼라를 곱합니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0))
 * val result = v * 2.0   // [2.0, 4.0, 6.0]
 * ```
 */
operator fun <N: Number> RealVector.times(scalar: Number): RealVector = mapMultiply(scalar.toDouble())

/**
 * 두 실수 벡터를 원소별로 나눕니다 (element-wise divide).
 *
 * ```kotlin
 * val v1 = realVectorOf(doubleArrayOf(8.0, 15.0))
 * val v2 = realVectorOf(doubleArrayOf(2.0, 3.0))
 * val result = v1 / v2   // [4.0, 5.0]
 * ```
 */
operator fun RealVector.div(v: RealVector): RealVector = ebeDivide(v)

/**
 * 실수 벡터를 스칼라로 나눕니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(2.0, 4.0, 6.0))
 * val result = v / 2.0   // [1.0, 2.0, 3.0]
 * ```
 */
operator fun <N: Number> RealVector.div(scalar: Number): RealVector = mapDivide(scalar.toDouble())

/**
 * RealVector를 ArrayRealVector로 변환합니다.
 *
 * ```kotlin
 * val v = realVectorOf(doubleArrayOf(1.0, 2.0))
 * val arrayVec = v.toArrayRealVector()
 * ```
 */
fun RealVector.toArrayRealVector(): ArrayRealVector = ArrayRealVector(this)
