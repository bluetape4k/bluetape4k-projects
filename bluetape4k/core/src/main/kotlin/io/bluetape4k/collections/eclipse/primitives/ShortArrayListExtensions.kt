package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asShort
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.ShortIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList

fun ShortArray.toShortArrayList(): ShortArrayList = ShortArrayList.newListWith(*this)

fun Iterable<Short>.toShortArrayList(): ShortArrayList =
    ShortArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Short>.toShortArrayList(): ShortArrayList = asIterable().toShortArrayList()

fun Iterable<Number>.asShortArrayList() = ShortArrayList().also { array ->
    forEach { number -> array.add(number.asShort()) }
}

inline fun shortArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Short,
): ShortArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return ShortArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

fun shortArrayListOf(vararg elements: Short): ShortArrayList =
    ShortArrayList.newListWith(*elements)

fun ShortIterable.toShortArrayList(): ShortArrayList = when (this) {
    is ShortArrayList -> this
    else -> ShortArrayList.newList(this)
}

fun ShortIterable.asIterator(): Iterator<Short> = iterator {
    val iter = shortIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun ShortIterable.asSequence(): Sequence<Short> = sequence {
    val iter = shortIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun ShortIterable.asIterable() = Iterable { asIterator() }
fun ShortIterable.asList() = asIterable().toList()
fun ShortIterable.asMutableList() = asIterable().toMutableList()
fun ShortIterable.asSet() = asIterable().toSet()
fun ShortIterable.asMutableSet() = asIterable().toMutableSet()

fun ShortIterable.asFastList() = asIterable().toFastList()
fun ShortIterable.asUnifiedSet() = asIterable().toUnifiedSet()
fun ShortIterable.asFixedSizeList() = asIterable().toFixedSizeList()

fun ShortIterable.maxOrNull() = if (isEmpty) null else max()
fun ShortIterable.minOrNull() = if (isEmpty) null else min()

fun ShortIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

fun ShortArray.toFastList(): FastList<Short> = asIterable().toFastList()
