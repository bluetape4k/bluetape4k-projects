package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.FloatIterable
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList

fun FloatArray.toFloatArrayList(): FloatArrayList = FloatArrayList.newListWith(*this)

fun Iterable<Float>.toFloatArrayList(): FloatArrayList =
    FloatArrayList().also { array ->
        forEach { array.add(it) }
    }

fun Sequence<Float>.toFloatArrayList(): FloatArrayList = asIterable().toFloatArrayList()

inline fun floatArrayList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> Float,
): FloatArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return FloatArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            this[index] = initializer(index)
        }
    }
}

fun floatArrayListOf(vararg elements: Float): FloatArrayList =
    FloatArrayList.newListWith(*elements)

fun FloatIterable.toFloatArrayList(): FloatArrayList = when (this) {
    is FloatArrayList -> this
    else -> FloatArrayList.newList(this)
}

fun FloatIterable.asIterator(): Iterator<Float> = iterator {
    val iter = floatIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun FloatIterable.asSequence(): Sequence<Float> = sequence {
    val iter = floatIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

fun FloatIterable.asIterable() = Iterable { asIterator() }
fun FloatIterable.asList() = asIterable().toList()
fun FloatIterable.asMutableList() = asIterable().toMutableList()
fun FloatIterable.asSet() = asIterable().toSet()
fun FloatIterable.asMutableSet() = asIterable().toMutableSet()

fun FloatIterable.asFastList() = asIterable().toFastList()
fun FloatIterable.asUnifiedSet() = asIterable().toUnifiedSet()

fun FloatIterable.maxOrNull() = if (isEmpty) null else max()
fun FloatIterable.minOrNull() = if (isEmpty) null else min()

fun FloatIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }
