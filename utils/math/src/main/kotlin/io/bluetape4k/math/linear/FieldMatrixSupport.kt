package io.bluetape4k.math.linear

import org.apache.commons.math3.FieldElement
import org.apache.commons.math3.linear.FieldMatrix

operator fun <T: FieldElement<T>> FieldMatrix<T>.get(row: Int, col: Int): T = getEntry(row, col)
operator fun <T: FieldElement<T>> FieldMatrix<T>.set(row: Int, col: Int, value: T) = setEntry(row, col, value)

operator fun <T: FieldElement<T>> FieldMatrix<T>.plus(vector: FieldMatrix<T>): FieldMatrix<T> = add(vector)
operator fun <T: FieldElement<T>> FieldMatrix<T>.plus(elem: T): FieldMatrix<T> = scalarAdd(elem)

operator fun <T: FieldElement<T>> FieldMatrix<T>.minus(vector: FieldMatrix<T>): FieldMatrix<T> = subtract(vector)
operator fun <T: FieldElement<T>> FieldMatrix<T>.minus(elem: T): FieldMatrix<T> = scalarAdd(elem.negate())

operator fun <T: FieldElement<T>> FieldMatrix<T>.times(vector: FieldMatrix<T>): FieldMatrix<T> = multiply(vector)
operator fun <T: FieldElement<T>> FieldMatrix<T>.times(item: T): FieldMatrix<T> = scalarMultiply(item)

// operator fun <T: FieldElement<T>> FieldMatrix<T>.div(vector: FieldMatrix<T>): FieldMatrix<T> = multiply()
operator fun <T: FieldElement<T>> FieldMatrix<T>.div(item: T): FieldMatrix<T> = scalarMultiply(item.reciprocal())
