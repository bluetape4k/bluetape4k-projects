package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asLong
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.LongIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

fun LongArray.toLongArrayList(): LongArrayList = LongArrayList.newListWith(*this)


fun Iterable<Long>.toLongArrayList(): LongArrayList =
    LongArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Long>.toLongArrayList(): LongArrayList = asIterable().toLongArrayList()

fun Iterable<Number>.asLongArrayList() = LongArrayList().also { array ->
    forEach { number -> array.add(number.asLong()) }
}

inline fun longArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Long,
): LongArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return LongArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(initializer(index))
        }
    }
}

fun longArrayListOf(vararg elements: Long): LongArrayList =
    LongArrayList.newListWith(*elements)

fun LongIterable.toLongArrayList(): LongArrayList = when (this) {
    is LongArrayList -> this
    else -> LongArrayList.newList(this)
}

fun LongIterable.asIterator(): Iterator<Long> = iterator {
    val iter = longIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun LongIterable.asSequence(): Sequence<Long> = sequence {
    val iter = longIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun LongIterable.asIterable() = Iterable { asIterator() }
fun LongIterable.asList() = asIterable().toList()
fun LongIterable.asMutableList() = asIterable().toMutableList()
fun LongIterable.asSet() = asIterable().toSet()
fun LongIterable.asMutableSet() = asIterable().toMutableSet()

fun LongIterable.asFastList() = asIterable().toFastList()
fun LongIterable.asUnifiedSet() = asIterable().toUnifiedSet()

fun LongIterable.maxOrNull() = if (isEmpty) null else max()
fun LongIterable.minOrNull() = if (isEmpty) null else min()

fun LongIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

fun LongArray.toFastList(): FastList<Long> = asIterable().toFastList()
