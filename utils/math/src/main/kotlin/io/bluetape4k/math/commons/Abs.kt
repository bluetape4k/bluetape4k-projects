package io.bluetape4k.math.commons

import java.math.BigDecimal
import kotlin.math.absoluteValue

/**
 * Double 값의 절대값을 반환합니다.
 *
 * ```kotlin
 * val result = (-3.14).abs()   // 3.14
 * val pos = 2.71.abs()         // 2.71
 * ```
 */
fun Double.abs(): Double = absoluteValue

/**
 * Float 값의 절대값을 반환합니다.
 *
 * ```kotlin
 * val result = (-1.5f).abs()   // 1.5
 * ```
 */
fun Float.abs(): Float = absoluteValue

/**
 * Long 값의 절대값을 반환합니다.
 *
 * ```kotlin
 * val result = (-100L).abs()   // 100
 * ```
 */
fun Long.abs(): Long = absoluteValue

/**
 * Int 값의 절대값을 반환합니다.
 *
 * ```kotlin
 * val result = (-42).abs()   // 42
 * ```
 */
fun Int.abs(): Int = absoluteValue

/**
 * Double 컬렉션의 각 요소에 절대값을 적용합니다.
 *
 * ```kotlin
 * val result = listOf(-1.0, 2.0, -3.0).abs().toList()   // [1.0, 2.0, 3.0]
 * ```
 */
@JvmName("absOfDouble")
fun Iterable<Double>.abs(): Iterable<Double> = map { it.absoluteValue }

/**
 * Float 컬렉션의 각 요소에 절대값을 적용합니다.
 *
 * ```kotlin
 * val result = listOf(-1.0f, 2.0f, -3.0f).abs().toList()   // [1.0, 2.0, 3.0]
 * ```
 */
@JvmName("absOfFloat")
fun Iterable<Float>.abs(): Iterable<Float> = map { it.absoluteValue }

/**
 * Long 컬렉션의 각 요소에 절대값을 적용합니다.
 *
 * ```kotlin
 * val result = listOf(-1L, 2L, -3L).abs().toList()   // [1, 2, 3]
 * ```
 */
@JvmName("absOfLong")
fun Iterable<Long>.abs(): Iterable<Long> = map { it.absoluteValue }

/**
 * Int 컬렉션의 각 요소에 절대값을 적용합니다.
 *
 * ```kotlin
 * val result = listOf(-1, 2, -3).abs().toList()   // [1, 2, 3]
 * ```
 */
@JvmName("absOfInt")
fun Iterable<Int>.abs(): Iterable<Int> = map { it.absoluteValue }

/**
 * BigDecimal 컬렉션의 각 요소에 절대값을 적용합니다.
 *
 * ```kotlin
 * val result = listOf(BigDecimal("-1.5"), BigDecimal("2.0")).abs().toList()
 * // [1.5, 2.0]
 * ```
 */
@JvmName("absOfBigDecimal")
fun Iterable<BigDecimal>.abs(): Iterable<BigDecimal> = map { it.abs() }
