package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.ByteIterable
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList

fun ByteArray.toByteArrayList(): ByteArrayList = ByteArrayList.newListWith(*this)

fun Iterable<Byte>.toByteArrayList(): ByteArrayList =
    ByteArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Byte>.toByteArrayList(): ByteArrayList = asIterable().toByteArrayList()

inline fun byteArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Byte,
): ByteArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return ByteArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            this[index] = initializer(index)
        }
    }
}

fun byteArrayListOf(vararg elements: Byte): ByteArrayList =
    ByteArrayList.newListWith(*elements)

fun ByteIterable.toByteArrayList(): ByteArrayList = when (this) {
    is ByteArrayList -> this
    else -> ByteArrayList.newList(this)
}

fun ByteIterable.asIterator(): Iterator<Byte> = iterator {
    val iter = byteIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun ByteIterable.asSequence(): Sequence<Byte> = sequence {
    val iter = byteIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun ByteIterable.asIterable(): Iterable<Byte> = Iterable { asIterator() }
fun ByteIterable.asList() = asIterable().toList()
fun ByteIterable.asMutableList() = asIterable().toMutableList()
fun ByteIterable.asSet() = asIterable().toSet()
fun ByteIterable.asMutableSet() = asIterable().toMutableSet()

fun ByteIterable.asFastList() = asIterable().toFastList()
fun ByteIterable.asUnifiedSet() = asIterable().toUnifiedSet()

fun ByteIterable.maxOrNull() = if (isEmpty) null else max()
fun ByteIterable.minOrNull() = if (isEmpty) null else min()

fun ByteIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }
