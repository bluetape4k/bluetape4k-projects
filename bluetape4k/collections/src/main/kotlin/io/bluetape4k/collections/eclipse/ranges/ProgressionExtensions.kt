package io.bluetape4k.collections.eclipse.ranges

import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

fun CharProgression.toCharArrayList(): CharArrayList {
    val array = CharArrayList(this.count())
    forEachIndexed { index, value ->
        array[index] = value
    }
    return array
}

fun IntProgression.toIntArrayList(): IntArrayList {
    val array = IntArrayList(this.count())
    forEachIndexed { index, value ->
        array[index] = value
    }
    return array
}

fun LongProgression.toLongArrayList(): LongArrayList {
    val array = LongArrayList(this.count())
    forEachIndexed { index, value ->
        array[index] = value
    }
    return array
}
