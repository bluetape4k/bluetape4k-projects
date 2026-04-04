package io.bluetape4k.math.linear

import org.apache.commons.math3.FieldElement
import org.apache.commons.math3.linear.FieldMatrix

/**
 * 인덱스로 필드 행렬의 원소를 가져옵니다.
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * val entry = m[0, 1]
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.get(row: Int, col: Int): T = getEntry(row, col)

/**
 * 인덱스로 필드 행렬의 원소를 설정합니다.
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * m[0, 1] = Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.set(row: Int, col: Int, value: T) = setEntry(row, col, value)

/**
 * 두 필드 행렬을 더합니다.
 *
 * ```kotlin
 * val m1: FieldMatrix<Fraction> = ...
 * val m2: FieldMatrix<Fraction> = ...
 * val result = m1 + m2
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.plus(vector: FieldMatrix<T>): FieldMatrix<T> = add(vector)

/**
 * 필드 행렬에 스칼라를 더합니다.
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * val result = m + Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.plus(elem: T): FieldMatrix<T> = scalarAdd(elem)

/**
 * 두 필드 행렬을 뺍니다.
 *
 * ```kotlin
 * val m1: FieldMatrix<Fraction> = ...
 * val m2: FieldMatrix<Fraction> = ...
 * val result = m1 - m2
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.minus(vector: FieldMatrix<T>): FieldMatrix<T> = subtract(vector)

/**
 * 필드 행렬에서 스칼라를 뺍니다.
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * val result = m - Fraction(1, 2)
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.minus(elem: T): FieldMatrix<T> = scalarAdd(elem.negate())

/**
 * 두 필드 행렬을 곱합니다.
 *
 * ```kotlin
 * val m1: FieldMatrix<Fraction> = ...
 * val m2: FieldMatrix<Fraction> = ...
 * val result = m1 * m2
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.times(vector: FieldMatrix<T>): FieldMatrix<T> = multiply(vector)

/**
 * 필드 행렬에 스칼라를 곱합니다.
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * val result = m * Fraction(2, 1)
 * ```
 */
operator fun <T: FieldElement<T>> FieldMatrix<T>.times(item: T): FieldMatrix<T> = scalarMultiply(item)

/**
 * 필드 행렬을 스칼라로 나눕니다 (역원을 곱합니다).
 *
 * ```kotlin
 * val m: FieldMatrix<Fraction> = ...
 * val result = m / Fraction(2, 1)   // 모든 원소를 2로 나눔
 * ```
 */
// operator fun <T: FieldElement<T>> FieldMatrix<T>.div(vector: FieldMatrix<T>): FieldMatrix<T> = multiply()
operator fun <T: FieldElement<T>> FieldMatrix<T>.div(item: T): FieldMatrix<T> = scalarMultiply(item.reciprocal())
