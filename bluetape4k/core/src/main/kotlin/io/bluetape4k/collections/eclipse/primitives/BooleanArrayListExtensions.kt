package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.BooleanIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList

fun BooleanArray.toBooleanArrayList(): BooleanArrayList =
    BooleanArrayList.newListWith(*this)

fun Iterable<Boolean>.toBooleanArrayList(): BooleanArrayList =
    when (this) {
        is Collection<Boolean> -> BooleanArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                   -> BooleanArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

fun Sequence<Boolean>.toBooleanArrayList(): BooleanArrayList = asIterable().toBooleanArrayList()

inline fun booleanArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Boolean,
): BooleanArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return BooleanArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

fun booleanArrayListOf(vararg elements: Boolean): BooleanArrayList =
    BooleanArrayList.newListWith(*elements)


fun BooleanIterable.toBooleanArrayList(): BooleanArrayList = when (this) {
    is BooleanArrayList -> this
    else -> BooleanArrayList.newList(this)
}

fun BooleanIterable.asIterator(): Iterator<Boolean> = object: Iterator<Boolean> {
    private val iter = booleanIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Boolean = iter.next()
}

fun BooleanIterable.asSequence(): Sequence<Boolean> = sequence {
    val iter = booleanIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}


fun BooleanIterable.asIterable(): Iterable<Boolean> = Iterable { asIterator() }
fun BooleanIterable.asList() = asIterable().toList()
fun BooleanIterable.asMutableList() = asIterable().toMutableList()
fun BooleanIterable.asSet() = asIterable().toSet()
fun BooleanIterable.asMutableSet() = asIterable().toMutableSet()

fun BooleanIterable.toFastList() = asIterable().toFastList()
fun BooleanIterable.toUnifiedSet() = asIterable().toUnifiedSet()
fun BooleanIterable.toFixedSizeList() = asIterable().toFixedSizeList()

fun BooleanArray.toFastList(): FastList<Boolean> = asIterable().toFastList()
