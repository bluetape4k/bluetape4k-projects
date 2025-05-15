package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.ImmutableLongArray
import com.danrusu.pods4k.immutableArrays.buildImmutableArray
import io.bluetape4k.support.requireGt

fun ImmutableLongArray.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): ImmutableArray<ImmutableLongArray> =
    windowed(size, step, partialWindows) { it }

inline fun <R> ImmutableLongArray.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (ImmutableLongArray) -> R,
): ImmutableArray<R> = buildImmutableArray {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    while (pos < this@windowed.size) {
        val window = ImmutableLongArray.Builder()
        repeat(size) {
            if (pos + it < this@windowed.size) {
                window.add(this@windowed[pos + it])
            }
        }
        val array = window.build()
        when {
            array.size == size -> add(transform(array))
            partialWindows     -> add(transform(array))
        }
        pos += step
    }
}

fun ImmutableLongArray.chunked(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableLongArray> =
    windowed(size, size, partialWindows)

inline fun <R> ImmutableLongArray.chunked(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableLongArray) -> R,
): ImmutableArray<R> =
    windowed(size, size, partialWindows, transform)


fun ImmutableLongArray.sliding(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableLongArray> =
    windowed(size, 1, partialWindows)


inline fun <R> ImmutableLongArray.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableLongArray) -> R,
): ImmutableArray<R> =
    windowed(size, 1, partialWindows, transform)
