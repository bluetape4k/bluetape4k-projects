package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asInt
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.IntIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList

fun IntArray.toIntArrayList(): IntArrayList = IntArrayList.newListWith(*this)

fun Iterable<Int>.toIntArrayList(): IntArrayList =
    when (this) {
        is Collection<Int> -> IntArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else               -> IntArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

fun Sequence<Int>.toIntArrayList(): IntArrayList = asIterable().toIntArrayList()

fun Iterable<Number>.asIntArrayList() = when (this) {
    is Collection<Number> -> IntArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asInt()) }
    }
    else                  -> IntArrayList().also { array ->
        forEach { number -> array.add(number.asInt()) }
    }
}

inline fun intArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Int,
): IntArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return IntArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

fun intArrayListOf(vararg elements: Int): IntArrayList =
    IntArrayList.newListWith(*elements)

fun IntIterable.toIntArrayList(): IntArrayList = when (this) {
    is IntArrayList -> this
    else -> IntArrayList.newList(this)
}

fun IntIterable.asIterator(): Iterator<Int> = object: Iterator<Int> {
    private val iter = intIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Int = iter.next()
}

fun IntIterable.asSequence(): Sequence<Int> = sequence {
    val iter = intIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun IntIterable.asIterable() = Iterable { asIterator() }
fun IntIterable.asList() = asIterable().toList()
fun IntIterable.asMutableList() = asIterable().toMutableList()
fun IntIterable.asSet() = asIterable().toSet()
fun IntIterable.asMutableSet() = asIterable().toMutableSet()

fun IntIterable.toFastList() = asIterable().toFastList()
fun IntIterable.toUnifiedSet() = asIterable().toUnifiedSet()
fun IntIterable.toFixedSizeList() = asIterable().toFixedSizeList()

fun IntIterable.maxOrNull() = if (isEmpty) null else max()
fun IntIterable.minOrNull() = if (isEmpty) null else min()

fun IntIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

fun IntArray.toFastList(): FastList<Int> = asIterable().toFastList()

//
