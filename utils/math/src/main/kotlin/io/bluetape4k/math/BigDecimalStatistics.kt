package io.bluetape4k.math

import java.math.BigDecimal

/**
 * BigDecimal 시퀀스의 합을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(BigDecimal("1.5"), BigDecimal("2.5")).sum()
 * // BigDecimal("4.0")
 * ```
 */
fun Sequence<BigDecimal>.sum(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, x -> acc + x }

/**
 * BigDecimal 컬렉션의 합을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(BigDecimal("1.5"), BigDecimal("2.5")).sum()
 * // BigDecimal("4.0")
 * ```
 */
fun Iterable<BigDecimal>.sum(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, x -> acc + x }

/**
 * BigDecimal 시퀀스의 평균을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(BigDecimal("1.0"), BigDecimal("3.0")).average()
 * // BigDecimal("2.0")
 * ```
 */
fun Sequence<BigDecimal>.average(): BigDecimal = sum() / count().toBigDecimal()

/**
 * BigDecimal 컬렉션의 평균을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(BigDecimal("1.0"), BigDecimal("3.0")).average()
 * // BigDecimal("2.0")
 * ```
 */
fun Iterable<BigDecimal>.average(): BigDecimal = sum() / count().toBigDecimal()
