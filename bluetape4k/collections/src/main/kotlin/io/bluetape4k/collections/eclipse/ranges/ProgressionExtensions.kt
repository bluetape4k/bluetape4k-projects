package io.bluetape4k.collections.eclipse.ranges

import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

fun CharProgression.toCharArrayList(): CharArrayList =
    CharArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }

fun IntProgression.toIntArrayList(): IntArrayList =
    IntArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }

fun LongProgression.toLongArrayList(): LongArrayList =
    LongArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }
