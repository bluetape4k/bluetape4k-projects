package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asChar
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.CharIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList

fun CharArray.toCharArrayList(): CharArrayList = CharArrayList.newListWith(*this)

fun Iterable<Char>.toCharArrayList(): CharArrayList =
    CharArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Char>.toCharArrayList(): CharArrayList = asIterable().toCharArrayList()

fun Iterable<Any>.asCharArrayList() = CharArrayList().also { array ->
    forEach { number -> array.add(number.asChar()) }
}

inline fun charArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Char,
): CharArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return CharArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(initializer(index))
        }
    }
}

fun charArrayListOf(vararg elements: Char): CharArrayList =
    CharArrayList.newListWith(*elements)

fun CharIterable.toCharArrayList(): CharArrayList = when (this) {
    is CharArrayList -> this
    else -> CharArrayList.newList(this)
}

fun CharIterable.asIterator(): Iterator<Char> = iterator {
    val iter = charIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun CharIterable.asSequence(): Sequence<Char> = sequence {
    val iter = charIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun CharIterable.asIterable(): Iterable<Char> = Iterable { asIterator() }
fun CharIterable.asList() = asIterable().toList()
fun CharIterable.asMutableList() = asIterable().toMutableList()
fun CharIterable.asSet() = asIterable().toSet()
fun CharIterable.asMutableSet() = asIterable().toMutableSet()

fun CharIterable.asFastList() = asIterable().toFastList()
fun CharIterable.asUnifiedSet() = asIterable().toUnifiedSet()

fun CharIterable.maxOrNull() = if (isEmpty) null else max()
fun CharIterable.minOrNull() = if (isEmpty) null else min()


fun CharArray.toFastList(): FastList<Char> = asIterable().toFastList()
