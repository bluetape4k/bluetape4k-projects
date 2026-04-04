package io.bluetape4k.math.transform

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.TransformUtils

/**
 * 배열의 모든 원소를 스칼라 `d`로 곱하여 스케일링합니다.
 *
 * ```kotlin
 * val data = doubleArrayOf(1.0, 2.0, 3.0)
 * val result = data.scale(2.0)   // [2.0, 4.0, 6.0]
 * ```
 */
fun DoubleArray.scale(d: Double): DoubleArray =
    TransformUtils.scaleArray(this, d)

/**
 * 복소수 배열의 모든 원소를 스칼라 `d`로 곱하여 스케일링합니다.
 *
 * ```kotlin
 * val data = arrayOf(Complex(1.0, 0.0), Complex(0.0, 1.0))
 * val result = data.scale(2.0)   // [Complex(2.0, 0.0), Complex(0.0, 2.0)]
 * ```
 */
fun Array<Complex>.scale(d: Double): Array<Complex> =
    TransformUtils.scaleArray(this, d)

/**
 * 복소수 배열을 실수부와 허수부의 2차원 배열로 변환합니다.
 *
 * ```kotlin
 * val data = arrayOf(Complex(1.0, 2.0), Complex(3.0, 4.0))
 * val result = data.toRealImaginaryArray()
 * // result[0] == [1.0, 3.0] (실수부)
 * // result[1] == [2.0, 4.0] (허수부)
 * ```
 */
fun Array<Complex>.toRealImaginaryArray(): Array<DoubleArray> =
    TransformUtils.createRealImaginaryArray(this)

/**
 * 실수부와 허수부의 2차원 배열을 복소수 배열로 변환합니다.
 *
 * ```kotlin
 * val data = arrayOf(doubleArrayOf(1.0, 3.0), doubleArrayOf(2.0, 4.0))
 * val result = data.toComplexArray()
 * // result == [Complex(1.0, 2.0), Complex(3.0, 4.0)]
 * ```
 */
fun Array<DoubleArray>.toComplexArray(): Array<Complex> =
    TransformUtils.createComplexArray(this)

/**
 * 2의 거듭제곱에 대한 정확한 로그 값(밑이 2인 로그)을 반환합니다.
 *
 * ```kotlin
 * val result = 8.exactLog2()   // 3 (2^3 = 8)
 * val result2 = 16.exactLog2() // 4 (2^4 = 16)
 * ```
 */
fun Int.exactLog2(): Int = TransformUtils.exactLog2(this)
