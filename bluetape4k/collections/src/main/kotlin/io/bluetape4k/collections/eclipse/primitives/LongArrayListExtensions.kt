package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.LongIterable
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

fun LongArray.toLongArrayList(): LongArrayList = LongArrayList.newListWith(*this)

fun Iterable<Long>.toLongArrayList(): LongArrayList =
    LongArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Long>.toLongArrayList(): LongArrayList = asIterable().toLongArrayList()

inline fun longArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Long,
): LongArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return LongArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            this[index] = initializer(index)
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

fun LongIterable.asIterable(): Iterable<Long> = Iterable { asIterator() }

fun LongIterable.asSequence(): Sequence<Long> = sequence {
    val iter = longIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}
