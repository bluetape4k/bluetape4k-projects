package io.bluetape4k.math.commons

import java.math.BigDecimal
import java.math.BigInteger

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = 3.0.square()   // 9.0
 * val result2 = 2.5.square()  // 6.25
 * ```
 */
fun Double.square(): Double = this * this

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = 3.0f.square()   // 9.0
 * ```
 */
fun Float.square(): Float = this * this

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = 4L.square()   // 16L
 * ```
 */
fun Long.square(): Long = this * this

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = 5.square()   // 25
 * ```
 */
fun Int.square(): Int = this * this

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = BigDecimal("3").square()   // BigDecimal("9")
 * ```
 */
fun BigDecimal.square(): BigDecimal = this * this

/**
 * 숫자의 제곱을 계산합니다.
 *
 * ```kotlin
 * val result = BigInteger.valueOf(6).square()   // BigInteger("36")
 * ```
 */
fun BigInteger.square() = this * this
