package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.DoubleIterable
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList

fun DoubleArray.toDoubleArrayList(): DoubleArrayList = DoubleArrayList.newListWith(*this)

fun Iterable<Double>.toDoubleArrayList(): DoubleArrayList =
    DoubleArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Double>.toDoubleArrayList(): DoubleArrayList = asIterable().toDoubleArrayList()

inline fun doubleArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Double,
): DoubleArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return DoubleArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            this[index] = initializer(index)
        }
    }
}

fun doubleArrayListOf(vararg elements: Double): DoubleArrayList =
    DoubleArrayList.newListWith(*elements)

fun DoubleIterable.toDoubleArrayList(): DoubleArrayList = when (this) {
    is DoubleArrayList -> this
    else -> DoubleArrayList.newList(this)
}

fun DoubleIterable.asIterator(): Iterator<Double> = iterator {
    val iter = doubleIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun DoubleIterable.asSequence(): Sequence<Double> = sequence {
    val iter = doubleIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun DoubleIterable.asIterable() = Iterable { asIterator() }
fun DoubleIterable.asList() = asIterable().toList()
fun DoubleIterable.asMutableList() = asIterable().toMutableList()
fun DoubleIterable.asSet() = asIterable().toSet()
fun DoubleIterable.asMutableSet() = asIterable().toMutableSet()

fun DoubleIterable.asFastList() = asIterable().toFastList()
fun DoubleIterable.asUnifiedSet() = asIterable().toUnifiedSet()

fun DoubleIterable.maxOrNull() = if (isEmpty) null else max()
fun DoubleIterable.minOrNull() = if (isEmpty) null else min()

fun DoubleIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }
