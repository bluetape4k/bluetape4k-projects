package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asDouble
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.DoubleIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList

fun DoubleArray.toDoubleArrayList(): DoubleArrayList = DoubleArrayList.newListWith(*this)

fun Iterable<Double>.toDoubleArrayList(): DoubleArrayList =
    when (this) {
        is Collection<Double> -> DoubleArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                  -> DoubleArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

fun Sequence<Double>.toDoubleArrayList() = asIterable().toDoubleArrayList()

fun Iterable<Number>.asDoubleArrayList() = when (this) {
    is Collection<Number> -> DoubleArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asDouble()) }
    }
    else                  -> DoubleArrayList().also { array ->
        forEach { number -> array.add(number.asDouble()) }
    }
}


inline fun doubleArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Double,
): DoubleArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return DoubleArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

fun doubleArrayListOf(vararg elements: Double): DoubleArrayList =
    DoubleArrayList.newListWith(*elements)

fun DoubleIterable.toDoubleArrayList(): DoubleArrayList = when (this) {
    is DoubleArrayList -> this
    else -> DoubleArrayList.newList(this)
}

fun DoubleIterable.asIterator(): Iterator<Double> = object: Iterator<Double> {
    private val iter = doubleIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Double = iter.next()
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

fun DoubleIterable.toFastList() = asIterable().toFastList()
fun DoubleIterable.toUnifiedSet() = asIterable().toUnifiedSet()
fun DoubleIterable.toFixedSizeList() = asIterable().toFixedSizeList()

fun DoubleIterable.maxOrNull() = if (isEmpty) null else max()
fun DoubleIterable.minOrNull() = if (isEmpty) null else min()

fun DoubleIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

fun DoubleArray.toFastList(): FastList<Double> = asIterable().toFastList()
