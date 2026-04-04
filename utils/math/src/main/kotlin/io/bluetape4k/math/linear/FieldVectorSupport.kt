package io.bluetape4k.math.linear

import org.apache.commons.math3.FieldElement
import org.apache.commons.math3.linear.FieldVector

/**
 * 인덱스로 필드 벡터의 원소를 가져옵니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * val entry = v[0]
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.get(index: Int): T = getEntry(index)

/**
 * 인덱스로 필드 벡터의 원소를 설정합니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * v[0] = Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.set(index: Int, elem: T) = setEntry(index, elem)

/**
 * 두 필드 벡터를 더합니다.
 *
 * ```kotlin
 * val v1: FieldVector<Fraction> = ...
 * val v2: FieldVector<Fraction> = ...
 * val result = v1 + v2
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.plus(vector: FieldVector<T>): FieldVector<T> = add(vector)

/**
 * 필드 벡터에 스칼라를 더합니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * val result = v + Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.plus(elem: T): FieldVector<T> = mapAdd(elem)

/**
 * 두 필드 벡터를 뺍니다.
 *
 * ```kotlin
 * val v1: FieldVector<Fraction> = ...
 * val v2: FieldVector<Fraction> = ...
 * val result = v1 - v2
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.minus(vector: FieldVector<T>): FieldVector<T> = subtract(vector)

/**
 * 필드 벡터에서 스칼라를 뺍니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * val result = v - Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.minus(elem: T): FieldVector<T> = mapSubtract(elem)

/**
 * 두 필드 벡터를 원소별로 곱합니다 (element-wise multiply).
 *
 * ```kotlin
 * val v1: FieldVector<Fraction> = ...
 * val v2: FieldVector<Fraction> = ...
 * val result = v1 * v2
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.times(vector: FieldVector<T>): FieldVector<T> = ebeMultiply(vector)

/**
 * 필드 벡터에 스칼라를 곱합니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * val result = v * Fraction(2, 1)
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.times(elem: T): FieldVector<T> = mapMultiply(elem)

/**
 * 두 필드 벡터를 원소별로 나눕니다 (element-wise divide).
 *
 * ```kotlin
 * val v1: FieldVector<Fraction> = ...
 * val v2: FieldVector<Fraction> = ...
 * val result = v1 / v2
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.div(vector: FieldVector<T>): FieldVector<T> = ebeDivide(vector)

/**
 * 필드 벡터를 스칼라로 나눕니다.
 *
 * ```kotlin
 * val v: FieldVector<Fraction> = ...
 * val result = v / Fraction(2, 1)
 * ```
 */
operator fun <T: FieldElement<T>> FieldVector<T>.div(elem: T): FieldVector<T> = mapDivide(elem)
