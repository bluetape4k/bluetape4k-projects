package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.BooleanIterable
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList

fun BooleanArray.toBooleanArrayList(): BooleanArrayList = BooleanArrayList.newListWith(*this)

fun Iterable<Boolean>.toBooleanArrayList(): BooleanArrayList =
    BooleanArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Boolean>.toBooleanArrayList(): BooleanArrayList = asIterable().toBooleanArrayList()

inline fun booleanArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Boolean,
): BooleanArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return BooleanArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            this[index] = initializer(index)
        }
    }
}

fun booleanArrayListOf(vararg elements: Boolean): BooleanArrayList =
    BooleanArrayList.newListWith(*elements)


fun BooleanIterable.toBooleanArrayList(): BooleanArrayList = when (this) {
    is BooleanArrayList -> this
    else -> BooleanArrayList.newList(this)
}

fun BooleanIterable.asIterator(): Iterator<Boolean> = iterator {
    val iter = booleanIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun BooleanIterable.asIterable(): Iterable<Boolean> = Iterable { asIterator() }

fun BooleanIterable.asSequence(): Sequence<Boolean> = sequence {
    val iter = booleanIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}
