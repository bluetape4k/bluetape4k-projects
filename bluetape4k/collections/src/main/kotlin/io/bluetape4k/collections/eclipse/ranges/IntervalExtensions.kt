package io.bluetape4k.collections.eclipse.ranges

import io.bluetape4k.collections.eclipse.primitives.intArrayList
import io.bluetape4k.collections.eclipse.primitives.longArrayList
import io.bluetape4k.collections.eclipse.primitives.toIntArrayList
import io.bluetape4k.collections.eclipse.primitives.toLongArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.list.primitive.IntInterval
import org.eclipse.collections.impl.list.primitive.LongInterval

fun intIntervalOf(start: Int, endInclusive: Int, step: Int = 1): IntInterval =
    IntInterval.fromToBy(start, endInclusive, step)

fun IntInterval.toIntArrayList(): IntArrayList = intArrayList(this.size()) { it }
fun IntInterval.toLongArrayList(): LongArrayList = longArrayList(this.size()) { it.toLong() }

inline fun IntInterval.forEach(crossinline block: (Int) -> Unit) {
    this.each { block(it) }
}

fun IntInterval.chunked(chunkSize: Int): Sequence<IntArrayList> = sequence {
    this@chunked
        .chunk(chunkSize)
        .forEach { chunked ->
            yield(chunked.toIntArrayList())
        }
}


fun intLongervalOf(start: Long, endInclusive: Long, step: Long = 1): LongInterval =
    LongInterval.fromToBy(start, endInclusive, step)

fun LongInterval.toLongArrayList(): LongArrayList = longArrayList(this.size()) { it.toLong() }

inline fun LongInterval.forEach(crossinline block: (Long) -> Unit) {
    this.each { block(it) }
}

fun LongInterval.chunked(chunkSize: Int): Sequence<LongArrayList> = sequence {
    this@chunked
        .chunk(chunkSize)
        .forEach { chunked ->
            yield(chunked.toLongArrayList())
        }
}
