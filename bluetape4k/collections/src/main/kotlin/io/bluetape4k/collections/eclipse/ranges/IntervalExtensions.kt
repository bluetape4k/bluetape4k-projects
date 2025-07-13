package io.bluetape4k.collections.eclipse.ranges

import io.bluetape4k.support.requireGt
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.list.primitive.IntInterval
import org.eclipse.collections.impl.list.primitive.LongInterval

fun intIntervalOf(start: Int, endInclusive: Int, step: Int = 1): IntInterval =
    IntInterval.fromToBy(start, endInclusive, step)

fun IntInterval.toIntArrayList(): IntArrayList =
    IntArrayList(size()).also { array ->
        forEach { element ->
            array.add(element)
        }
    }

fun IntInterval.toLongArrayList(): LongArrayList =
    LongArrayList(size()).also { array ->
        forEach { element ->
            array.add(element.toLong())
        }
    }

inline fun IntInterval.forEach(crossinline block: (Int) -> Unit) {
    this.each { block(it) }
}

inline fun IntInterval.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> = sequence {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    val intervalSize = this@windowed.size()
    while (pos < intervalSize) {
        val window = IntArrayList()
        repeat(size) {
            if (pos + it < intervalSize) {
                window.add(this@windowed[pos + it])
            }
        }
        when {
            window.size() == size -> yield(transform(window))
            partialWindows -> yield(transform(window))
        }
        pos += step
    }
}

inline fun IntInterval.chunked(
    chunkSize: Int,
    partialWindows: Boolean = true,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> =
    windowed(chunkSize, chunkSize, partialWindows, transform)

inline fun IntInterval.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> =
    windowed(size, 1, partialWindows, transform)

fun longIntervalOf(start: Long, endInclusive: Long, step: Long = 1): LongInterval =
    LongInterval.fromToBy(start, endInclusive, step)

fun LongInterval.toLongArrayList(): LongArrayList =
    LongArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }

inline fun LongInterval.forEach(crossinline block: (Long) -> Unit) {
    this.each { block(it) }
}

inline fun LongInterval.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> = sequence {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    val intervalSize = this@windowed.size()
    while (pos < intervalSize) {
        val window = LongArrayList()
        repeat(size) {
            if (pos + it < intervalSize) {
                window.add(this@windowed[pos + it])
            }
        }
        when {
            window.size() == size -> yield(transform(window))
            partialWindows -> yield(transform(window))
        }
        pos += step
    }
}

inline fun LongInterval.chunked(
    chunkSize: Int,
    partialWindows: Boolean = true,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> =
    windowed(chunkSize, chunkSize, partialWindows, transform)

inline fun LongInterval.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> =
    windowed(size, 1, partialWindows, transform)
