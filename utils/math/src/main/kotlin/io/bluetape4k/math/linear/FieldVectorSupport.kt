package io.bluetape4k.math.linear

import org.apache.commons.math3.FieldElement
import org.apache.commons.math3.linear.FieldVector

operator fun <T: FieldElement<T>> FieldVector<T>.get(index: Int): T = getEntry(index)
operator fun <T: FieldElement<T>> FieldVector<T>.set(index: Int, elem: T) = setEntry(index, elem)

operator fun <T: FieldElement<T>> FieldVector<T>.plus(vector: FieldVector<T>): FieldVector<T> = add(vector)
operator fun <T: FieldElement<T>> FieldVector<T>.plus(elem: T): FieldVector<T> = mapAdd(elem)

operator fun <T: FieldElement<T>> FieldVector<T>.minus(vector: FieldVector<T>): FieldVector<T> = subtract(vector)
operator fun <T: FieldElement<T>> FieldVector<T>.minus(elem: T): FieldVector<T> = mapSubtract(elem)

operator fun <T: FieldElement<T>> FieldVector<T>.times(vector: FieldVector<T>): FieldVector<T> = ebeMultiply(vector)
operator fun <T: FieldElement<T>> FieldVector<T>.times(elem: T): FieldVector<T> = mapMultiply(elem)

operator fun <T: FieldElement<T>> FieldVector<T>.div(vector: FieldVector<T>): FieldVector<T> = ebeDivide(vector)
operator fun <T: FieldElement<T>> FieldVector<T>.div(elem: T): FieldVector<T> = mapDivide(elem)
