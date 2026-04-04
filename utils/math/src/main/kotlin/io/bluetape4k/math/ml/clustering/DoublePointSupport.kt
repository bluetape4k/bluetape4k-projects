package io.bluetape4k.math.ml.clustering

import org.apache.commons.math3.ml.clustering.DoublePoint

/**
 * x, y 좌표로 2차원 DoublePoint를 생성합니다.
 *
 * ```kotlin
 * val point = doublePointOf(3.0, 4.0)
 * // point.point == [3.0, 4.0]
 * ```
 */
fun doublePointOf(x: Double, y: Double): DoublePoint =
    DoublePoint(doubleArrayOf(x, y))

/**
 * Double 배열로 DoublePoint를 생성합니다.
 *
 * ```kotlin
 * val point = doublePointOf(doubleArrayOf(1.0, 2.0, 3.0))
 * // point.point == [1.0, 2.0, 3.0]
 * ```
 */
fun doublePointOf(values: DoubleArray): DoublePoint = DoublePoint(values)

/**
 * 숫자 가변 인자로 DoublePoint를 생성합니다.
 *
 * ```kotlin
 * val point = doublePointOf(1, 2, 3)
 * // point.point == [1.0, 2.0, 3.0]
 * ```
 */
fun <N: Number> doublePointOf(vararg values: N): DoublePoint =
    DoublePoint(values.map { it.toDouble() }.toDoubleArray())

/**
 * Double 배열을 DoublePoint로 변환합니다.
 *
 * ```kotlin
 * val point = doubleArrayOf(1.0, 2.0).toDoublePoint()
 * // point.point == [1.0, 2.0]
 * ```
 */
fun DoubleArray.toDoublePoint(): DoublePoint = DoublePoint(this)
